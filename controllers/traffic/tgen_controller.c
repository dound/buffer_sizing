/**
 * Filename: tgen_controller.c
 * Purpose: defines a server which listens for a connection from the demo controller and
 *          listens for commands for creating and destroying traffic
 *          generators.
 */

#ifdef _LINUX_
#include <stdint.h> /* uintX_t */
#endif
#include <arpa/inet.h>
#include <errno.h>
#include <netinet/in.h>
#include <pthread.h>
#include <signal.h>
#include <stdarg.h>
#include <string.h>
#include <sys/time.h>
#include <time.h>
#include <unistd.h>
#include <wait.h>
#include "../common.h"
#include "../io_wrapper.h"

#define STR_VERSION "0.01b"

#define STR_USAGE "\
Traffic Generator Controller v%s\n\
%s: [-?]\n\
  -?, -help:       displays this help\n\
  -l, -listen:     the port to listen for connections on\n\
  -s, -server:     target iperf server\n\
  -o, -offset:     sets the offset for what ports to use\n\
  -debug:          run sleep instead of iperf\n\
  -v, -verbose:    verbosely print information to standard out\n\
\n\
Example: ./tgen_controller -port 10273 -server 127.0.0.1 -offset 0 -debug\n"

/** port to contact the server on */
#define DEFAULT_PORT 10273

#define MAX_FLOWS 100
#define MIN_PORT 5001
static unsigned numFlows = 0;
static int tgen_pid[MAX_FLOWS];
static char cmd[256];
static int portOffset;
static int debugApp = 0;

/** encapsulates a message to a client's controller */
typedef struct {
    uint8_t code;
    uint32_t val;
} __attribute__ ((packed)) control_t;

typedef enum {
    CODE_SET_N=0,
    CODE_SET_TGEN=1,
} code_t;

static uint16_t server_port;
static char iperf_server_ip[32];

static void controller_main();
static void setNumFlows(unsigned n);

int main( int argc, char** argv ) {
    int verbose;
    print_init("Traffic Controller Server");

    /* default values for command-line parameters */
    server_port = DEFAULT_PORT;
    iperf_server_ip[0] = '\0';
    portOffset = -1;

    /* ignore the broken pipe signal */
    signal( SIGPIPE, SIG_IGN );

    /* parse command-line arguments */
    unsigned i;
    for( i=1; i<argc || argc<=1; i++ ) {
        if( argc<=1 || str_matches(argv[i], 5, "-?", "-help", "--help", "help", "?") ) {
            printf( STR_USAGE, STR_VERSION, (argc>0) ? argv[0] : "tgen_controller" );
            return 0;
        }
        else if( str_matches(argv[i], 3, "-s", "-server", "--server") ) {
            i += 1;
            if( i == argc ) {
                print("Error: -server requires an IP address to be specified" );
                return -1;
            }
            struct in_addr in_ip;
            if( inet_aton(argv[i],&in_ip) == 0 ) {
                print("Error: %s is not a valid IP address", argv[i] );
                return -1;
            }
            char* strTmp = inet_ntoa(in_ip);
            strncpy(iperf_server_ip, strTmp, 32);
        }
        else if( str_matches(argv[i], 1, "-debug") ) {
            debugApp = 1;
        }
        else if( str_matches(argv[i], 3, "-p", "-port", "--port") ) {
            i += 1;
            if( i == argc ) {
                print("Error: -port requires a port number to be specified" );
                return -1;
            }
            uint32_t val = strtoul( argv[i], NULL, 10 );
            if( val==0 || val > 65535 ) {
                print("Error: %u is not a valid port", val );
                return -1;
            }
            server_port = val;
        }
        else if( str_matches(argv[i], 3, "-o", "-offset", "--offset") ) {
            i += 1;
            if( i == argc ) {
                print("Error: -offset requires a number to be specified" );
                return -1;
            }
            uint32_t val = strtoul( argv[i], NULL, 10 );
            portOffset = val;
        }
        else if( str_matches(argv[i], 3, "-v", "-verbose", "--verbose") ) {
            verbose = 1;
        }
    }
    print_set_verbosity(verbose);

    if( iperf_server_ip[0] == '\0' ) {
        print("Error: -server is a required argument; you must supply an iperf server IP" );
        exit( 1 );
    }

    if( portOffset==-1 ) {
        print("Error: -offset is a required argument; you must supply an iperf port offset" );
        exit( 1 );
    }

    /* listen for commands from the server */
    while(1)
        controller_main();

    return 0;
}

/** listens for incoming connections from the master who will send commands */
static void controller_main() {
    struct sockaddr_in servaddr;
    struct sockaddr_in cliaddr;
    socklen_t cliaddr_len;
    int server_fd, client_fd, len, val;

    /* initialize the server's info */
    servaddr.sin_family = AF_INET;
    servaddr.sin_addr.s_addr = htonl(INADDR_ANY);
    servaddr.sin_port = htons( server_port );

    /* make a TCP socket for the new flow */
    if( (server_fd = socket( AF_INET, SOCK_STREAM, IPPROTO_TCP )) == -1 ) {
        perror( "Error: unable to create TCP socket for router controller" );
        exit( 1 );
    }

    /* permit the socket to reuse the port even if it is already in use */
    val = 1;
    setsockopt(server_fd, SOL_SOCKET, SO_REUSEADDR, &val, sizeof(val));

    /* bind to the port */
    if( bind(server_fd, (struct sockaddr *)&servaddr, sizeof(servaddr)) < 0 ) {
        perror( "Error: unable to bind TCP socket for router controller" );
        exit(1);
    }

    /* listen for incoming connections */
    print("listening for an incoming connection request to TCP port %u", server_port);
    if( listen(server_fd, 1) < 0 ) {
        perror( "Error: unable to listen" );
        exit(1);
    }

    /* loop forever */
    while( 1 ) {
        print("waiting for client to connect ...");
        cliaddr_len = sizeof(cliaddr);
        if( (client_fd=accept(server_fd, (struct sockaddr*)&cliaddr, &cliaddr_len)) < 0 ) {
            perror( "Error: accept failed" );
            continue;
        }
        print("now connected to client at %s", inet_ntoa(cliaddr.sin_addr));

        /* wait for control packets */
        control_t packet;
        while( (len=readn(client_fd, &packet, sizeof(packet))) ) {
            if( len < 0 )
                break;

            int ret = 0;
            packet.val = ntohl( packet.val );
            switch( packet.code ) {
            case CODE_SET_N:
                setNumFlows(packet.val);
                break;

            case CODE_SET_TGEN:
                print("received set tgen command (unsupported)");
                break;

            default:
                print("received unexpected packet code %u", packet.code);
            }
            if( ret == -1 ) {
                print("failed to write (terminating this connection)");
                break;
            }
        }

        close( client_fd );
        close( server_fd );
        print("connection closed by peer");
        break;
    }

    /* kill all leftover flows */
    setNumFlows(0);
}

/** Sets the number of flows. */
static void setNumFlows(unsigned n) {
    print("N=%u, requested N=%u", numFlows, n);

    /* remove flows if we have too many */
    while( numFlows > n ) {
        numFlows -= 1;
        int pid = tgen_pid[numFlows];

        /* send SIGKILL to the process */
        snprintf(cmd, 256, "kill -9 %u", pid);
        system(cmd);
        print("killed pid %u", pid);

        /* clean up the process */
        int junk;
        waitpid(pid, &junk, 0);
        print("pid %u has been cleaned up", pid);
    }

    /* add flows if we don't have enough */
    while( numFlows < n ) {
        if( n >= MAX_FLOWS ) {
            print("Warning: too many flows requested (max=%u, requested=%u)", MAX_FLOWS, n );
            return;
        }

        int pid = fork();
        if( pid == 0 ) {
            /* reuse this process to run the app */
            if( debugApp )
                execl("/bin/sleep", "sleep",  "86400", (char*)0);
            else {
                snprintf(cmd, 256, "%u", MIN_PORT+n-1+portOffset);
                execl("/usr/local/bin/iperf", "iperf", "-c", iperf_server_ip, "-p", cmd, "-t", "86400", "-i", "5", (char*)0);
            }

            print("premature termiation?");
            exit(0);
        }
        else {
            tgen_pid[numFlows++] = pid;
            print("spawned pid %u", pid);
        }
    }

    print("# of flows = %u", numFlows);
}

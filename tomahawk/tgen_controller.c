/**
 * Filename: tgen_controller.c
 * Purpose: defines a client which connects to the demo controller and
 *          listens for commands for creating and destroying traffic
 *          generators.
 */

#ifdef _LINUX_
#include <stdint.h> /* uintX_t */
#endif
#include <arpa/inet.h>
#include <errno.h>
#include <pthread.h>
#include <signal.h>
#include <sys/time.h>
#include <time.h>
#include <unistd.h>
#include <wait.h>
#include "common.h"
#include "io_wrapper.h"

#define STR_VERSION "0.01b"

#define STR_USAGE "\
Traffic Generator Controller v%s\n\
%s: [-?]\n\
  -?, -help:       displays this help\n\
  -d, -dst, -ip:   sets the server IP address to connect to\n\
  -p, -port:       sets the server port to connect to\n\
  -o, -offset:     sets the offset for what ports to use\n\
  -debug:          run sleep instead of iperf\n"

/** number of seconds between updates */
#define UPDATE_INTERVAL_SEC 0
#define UPDATE_INTERVAL_NSEC (1000 * 1000) /* one per millisecond */

/** port to contact the server on */
#define DEFAULT_PORT 10273

#define MAX_FLOWS 100
#define MIN_PORT 5001
static unsigned numFlows = 0;
static int tgen_pid[MAX_FLOWS];
static char cmd[256];
static int portOffset = 0;
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

static uint32_t server_ip;
static uint16_t server_port;

static void controller_main();
static void setNumFlows(unsigned n);

int main( int argc, char** argv ) {
    server_ip = 0;
    server_port = DEFAULT_PORT;

    /* ignore the broken pipe signal */
    signal( SIGPIPE, SIG_IGN );

    /* parse command-line arguments */
    unsigned i;
    for( i=1; i<argc || argc<=1; i++ ) {
        if( argc<=1 || str_matches(argv[i], 5, "-?", "-help", "--help", "help", "?") ) {
            printf( STR_USAGE, STR_VERSION, (argc>0) ? argv[0] : "tgen_controller" );
            return 0;
        }
        else if( str_matches(argv[i], 5, "-d", "-dst", "--dst", "-ip", "--ip") ) {
            i += 1;
            if( i == argc ) {
                fprintf( stderr, "Error: -ip requires an IP address to be specified\n" );
                return -1;
            }
            struct in_addr in_ip;
            if( inet_aton(argv[i],&in_ip) == 0 ) {
                fprintf( stderr, "Error: %s is not a valid IP address\n", argv[i] );
                return -1;
            }
            server_ip = in_ip.s_addr;
        }
        else if( str_matches(argv[i], 1, "-debug") ) {
            debugApp = 1;
        }
        else if( str_matches(argv[i], 3, "-p", "-port", "--port") ) {
            i += 1;
            if( i == argc ) {
                fprintf( stderr, "Error: -port requires a port number to be specified\n" );
                return -1;
            }
            uint32_t val = strtoul( argv[i], NULL, 10 );
            if( val==0 || val > 65535 ) {
                fprintf( stderr, "Error: %u is not a valid port\n", val );
                return -1;
            }
            server_port = val;
        }
        else if( str_matches(argv[i], 3, "-o", "-offset", "--offset") ) {
            i += 1;
            if( i == argc ) {
                fprintf( stderr, "Error: -offset requires a number to be specified\n" );
                return -1;
            }
            uint32_t val = strtoul( argv[i], NULL, 10 );
            portOffset = val;
        }
    }
    if( server_ip==0 ) {
        fprintf( stderr, "Error: -dst is a required argument; you must supply a server IP\n" );
        exit( 1 );
    }

    /* listen for commands from the server */
    controller_main();
    return 0;
}

/** listens for incoming connections from the master who will send commands */
static void controller_main() {
  while(1) {
    struct sockaddr_in servaddr;
    int fd, len;

    /* initialize the server's info */
    servaddr.sin_family = AF_INET;
    servaddr.sin_addr.s_addr = server_ip;
    servaddr.sin_port = htons( server_port );

    /* make a TCP socket */
    if( (fd = socket( AF_INET, SOCK_STREAM, IPPROTO_TCP )) == -1 ) {
        perror( "Error: unable to create TCP socket for tgen controller" );
        exit( 1 );
    }

    /* connect to the server */
    fprintf( stderr, "tgen controller Trying to connect to the demo controller\n" );
    if( connect( fd, (struct sockaddr*)&servaddr, sizeof(servaddr) ) != 0 ) {
        perror( "Error: connect for tgen controller failed" );
        sleep( 5 );
        close( fd );
        continue;
    }

    while( 1 ) {
        fprintf( stderr, "tgen controller Connected to the master\n" );

        /* wait for control packets */
        control_t packet;
        while( (len=readn(fd, &packet, sizeof(packet))) ) {
            if( len < 0 )
                break;

            int ret = 0;
            packet.val = ntohl( packet.val );
            switch( packet.code ) {
            case CODE_SET_N:
                setNumFlows(packet.val);
                break;

            case CODE_SET_TGEN:
                fprintf(stderr,"warning: tgen controller got set tgen command (unsupported)\n");;
                break;

            default:
                fprintf( stderr, "tgen controller got unexpected packet code %u\n", packet.code );
            }
            if( ret == -1 ) {
                fprintf( stderr, "tgen controller failed to write (will terminate)\n" );
                break;
            }
        }

        close( fd );
        fprintf( stderr, "master connection closed to tgen controller (goodbye)\n" );
        break;
    }

    /* kill all leftover flows */
    setNumFlows(0);
  }
}

/** Setsthe number of flows. */
static void setNumFlows(unsigned n) {
    fprintf(stderr, "N=%u, requested N=%u\n", numFlows, n);

    /* remove flows if we have too many */
    while( numFlows > n ) {
        numFlows -= 1;
        int pid = tgen_pid[numFlows];

        /* send SIGKILL to the process */
        snprintf(cmd, 256, "kill -9 %u", pid);
        system(cmd);
        fprintf(stderr, "killed pid %u\n", pid);

        /* clean up the process */
        int junk;
        waitpid(pid, &junk, 0);
        fprintf(stderr, "pid %u has been cleaned up\n", pid);
    }

    /* add flows if we don't have enough */
    while( numFlows < n ) {
        int pid = fork();
        if( pid == 0 ) {
            if( debugApp )
                snprintf(cmd, 256, "sleep 86400");
            else
                snprintf(cmd, 256,
                         "iperf -c 64.57.23.34 -p %u -t 86400",
                         MIN_PORT+n-1+portOffset);

            system(cmd);
            fprintf(stderr, "premature termiation?\n");
            exit(0);
        }
        else {
            tgen_pid[numFlows++] = pid;
            fprintf(stderr, "spawned pid %u\n", pid);
        }
    }

    fprintf(stderr, "tgen controller # of flows = %u\n", numFlows);
}

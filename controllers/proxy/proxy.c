/**
 * Filename: proxy.c
 * Purpose: defines an in-between connects to a client and a server and fowards
 * all traffic from one to the other.
 */

#ifdef _LINUX_
#include <stdint.h> /* uintX_t */
#endif
#include <arpa/inet.h>
#include <errno.h>
#include <pthread.h>
#include <signal.h>
#include <stdarg.h>
#include <string.h>
#include <unistd.h>
#include "../common.h"
#include "../io_wrapper.h"

#define STR_VERSION "0.01b"

#define STR_USAGE "\
Bidirectional Proxy v%s\n\
  -?, -help:     displays this help\n\
  -p, -port:     the port to listen for connections on and to connect to the server on\n\
  -s, -server:   IP address of the server\n"

static uint16_t port;
static uint32_t server_ip;
static char str_server_ip[32];
static int remote_client_fd = -1;
static int remote_server_fd = -1;
static int remote_client_ready = 0;
static int remote_server_ready = 0;
static pthread_mutex_t lock_rc = PTHREAD_MUTEX_INITIALIZER;
static pthread_mutex_t lock_rs = PTHREAD_MUTEX_INITIALIZER;
static pthread_cond_t cond     = PTHREAD_COND_INITIALIZER;

static void* proxy_server( void* nil );
static void proxy_client();
static void forwardAllData(int fdFrom, const char* strFrom, int fdTo, const char* strTo);
static int isAlive();

int main( int argc, char** argv ) {
    print_init("Proxy");
    print_set_verbosity(0);

    /* default values for command-line parameters */
    port = 10272;
    server_ip = 0;

    /* ignore the broken pipe signal */
    signal( SIGPIPE, SIG_IGN );

    /* parse command-line arguments */
    unsigned i;
    for( i=1; i<argc || argc<=1; i++ ) {
        if( argc<=1 || str_matches(argv[i], 5, "-?", "-help", "--help", "help", "?") ) {
            printf( STR_USAGE, STR_VERSION );
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
            server_ip = in_ip.s_addr;
            strncpy( str_server_ip, inet_ntoa(in_ip), 32 );
        }
        else if( str_matches(argv[i], 3, "-p", "-port", "--port") ) {
            i += 1;
            if( i == argc ) {
                print("Error: -port requires a port number to be specified");
                return -1;
            }
            uint32_t val = strtoul( argv[i], NULL, 10 );
            if( val==0 || val > 65535 ) {
                print("Error: %u is not a valid port", val);
                return -1;
            }
            port = val;
        }
    }

    if( server_ip == 0 ) {
        print("Error: -server must be specified");
        exit(1);
    }

    /* listen for commands from the server */
    pthread_t tid;
    if( 0 != pthread_create( &tid, NULL, proxy_server, NULL ) ) {
        print("Error: unable to start the main controller thread");
        return -1;
    }

    /* listen for event capture traffic and tell the client about it */
    while(1)
        proxy_client();

    return 0;
}

/** listens for incoming connections from the master who will send commands */
static void* proxy_server( void* nil ) {
    struct sockaddr_in servaddr;
    struct sockaddr_in cliaddr;
    socklen_t cliaddr_len;
    int servfd, val;

    /* initialize the server's info */
    servaddr.sin_family = AF_INET;
    servaddr.sin_addr.s_addr = htonl(INADDR_ANY);
    servaddr.sin_port = htons(port);

    /* make a TCP socket to listen with */
    if( (servfd = socket( AF_INET, SOCK_STREAM, IPPROTO_TCP )) == -1 ) {
        perror( "Error: unable to create TCP socket for proxy server" );
        exit( 1 );
    }

    /* permit the socket to reuse the port even if it is already in use */
    val = 1;
    setsockopt(servfd, SOL_SOCKET, SO_REUSEADDR, &val, sizeof(val));

    /* bind to the port */
    if( bind(servfd, (struct sockaddr *)&servaddr, sizeof(servaddr)) < 0 ) {
        perror( "Error: unable to bind TCP socket for proxy server" );
        exit(1);
    }

    /* listen for incoming connections */
    print("listening for an incoming connection request to TCP port %u", port);
    if( listen(servfd, 1) < 0 ) {
        perror( "Error: unable to listen" );
        exit( 1 );
    }

    /* loop forever */
    while( 1 ) {
        print("waiting for client to connect ...");
        cliaddr_len = sizeof(cliaddr);
        if( (remote_client_fd=accept(servfd, (struct sockaddr*)&cliaddr, &cliaddr_len)) < 0 ) {
            perror( "Error: accept failed" );
            continue;
        }
        print("now connected to client at %s", inet_ntoa(cliaddr.sin_addr));

        /* wait until the other is ready too */
        pthread_mutex_lock(&lock_rc);
        remote_client_ready = 1;
        pthread_cond_signal(&cond);
        pthread_mutex_lock(&lock_rs);
        while( !isAlive() ) {
            pthread_mutex_unlock(&lock_rs);
            pthread_cond_wait(&cond, &lock_rc);
            pthread_mutex_lock(&lock_rs);
        }
        pthread_mutex_unlock(&lock_rs);
        /* continue to hold lock_rc while remote client connection is active! */

        /* send all data arriving from the remote client to the remote server */
        forwardAllData(remote_client_fd, "remote client", remote_server_fd, "remote server");

        /* cleanup the rc connection */
        close(remote_client_fd);
        remote_client_fd = -1;
        remote_client_ready = 0;
        print("connection to client at %s closed", inet_ntoa(cliaddr.sin_addr));

        /* release the lock so a new round of connections can be setup */
        pthread_mutex_unlock(&lock_rc);
    }

    close(servfd);
    return NULL;
}

static void proxy_client() {
    struct sockaddr_in servaddr;

    /* initialize the server's info */
    servaddr.sin_family = AF_INET;
    servaddr.sin_addr.s_addr = server_ip;
    servaddr.sin_port = htons(port);

    /* make a TCP socket for the new flow */
    if( (remote_server_fd = socket( AF_INET, SOCK_STREAM, IPPROTO_TCP )) == -1 ) {
        perror( "Error: unable to create TCP socket for proxy client" );
        exit( 1 );
    }

    /* connect to the server */
    print( "Trying to connect to the remote server at %s", str_server_ip );
    if( connect( remote_server_fd, (struct sockaddr*)&servaddr, sizeof(servaddr) ) != 0 ) {
        perror( "Error: connect to remote server failed" );
        close( remote_server_fd );
        remote_server_fd = -1;
        sleep( 2 );
        return;
    }

    print( "Connected to the remote server at %s", str_server_ip );

    /* wait until the other is ready too */
    pthread_mutex_lock(&lock_rc);
    pthread_mutex_lock(&lock_rs);
    remote_server_ready = 1;
    pthread_cond_signal(&cond);
    while( !isAlive() ) {
        pthread_mutex_unlock(&lock_rs);
        pthread_cond_wait(&cond, &lock_rc);
        pthread_mutex_lock(&lock_rs);
    }
    pthread_mutex_unlock(&lock_rc);
    /* continue to hold lock_rs while remote server connection is live! */

    /* send all data arriving from the remote server to the remote client */
    forwardAllData(remote_server_fd, "remote server", remote_client_fd, "remote client");

    /* cleanup the rs connection */
    close( remote_server_fd );
    remote_server_fd = -1;
    remote_server_ready = 0;

    /* release the lock so a new round of connections can be setup */
    pthread_mutex_unlock(&lock_rs);
}

static void forwardAllData(int fdFrom, const char* strFrom, int fdTo, const char* strTo) {
#define BUF_SIZE 1500
    uint8_t buf[BUF_SIZE];
    int len;

    /* wait for data to forward */
    while( (len=read(fdFrom, buf, BUF_SIZE)) ) {
        if( len < 0 ) {
            if( errno == EINTR )
                continue;
            else {
                print("error when trying to read from %s", strFrom);
                return;
            }
        }
        else if(len == 0) {
            print("EOF received from %s", strFrom);
            break;
        }
        else {
            if( writen(fdTo, buf, len) != len ) {
                print("error when trying to forward data from %s to %s", strFrom, strTo);
                break;
            }
        }
    }
}

static int isAlive() {
    return remote_client_ready && remote_server_ready;
}

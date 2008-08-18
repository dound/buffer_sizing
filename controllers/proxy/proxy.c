/**
 * Filename: proxy.c
 * Purpose: defines a bi-directional proxy which pipes all received data from a
 * client (which connects to the proxy) to a server (which the proxy connects
 * to) and vice-versa.
 */

#ifdef _LINUX_
#include <stdint.h> /* uintX_t */
#endif
#include <arpa/inet.h>
#include <errno.h>
#include <signal.h>
#include "../common.h"
#include "../io_wrapper.h"

#define STR_VERSION "0.01b"

#define STR_USAGE "\
Bidirectional Proxy v%s\n\
  -?, -help:     displays this help\n\
  -p, -port:     the port to listen for connections on and to connect to the server on\n\
  -s, -server:   IP address of the server\n"

static void proxyLoop(uint32_t server_ip, uint16_t port);

int main( int argc, char** argv ) {
    uint32_t server_ip;
    uint16_t port;

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

    /* pipe bi-directional data between a client and server */
    while(1) {
        proxyLoop(server_ip, port);

        print("pausing for 5 seconds before trying to connect again");
        sleep(5);
    }

    return 0;
}

/** Listens for an incoming connection and returns the fd associated with client
    who connects, or < 0 on error. */
static int waitForClient(uint16_t port) {
    struct sockaddr_in servaddr;
    struct sockaddr_in cliaddr;
    socklen_t cliaddr_len;
    int servfd, clifd, val;

    /* initialize the server's info */
    servaddr.sin_family = AF_INET;
    servaddr.sin_addr.s_addr = htonl(INADDR_ANY);
    servaddr.sin_port = htons(port);

    /* make a TCP socket to listen with */
    if( (servfd = socket( AF_INET, SOCK_STREAM, IPPROTO_TCP )) == -1 ) {
        perror( "Error: unable to create TCP socket for proxy server" );
        exit(1);
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
        exit(1);
    }

    /* wait for a client */
    print("waiting for client to connect ...");
    cliaddr_len = sizeof(cliaddr);
    clifd = accept(servfd, (struct sockaddr*)&cliaddr, &cliaddr_len);

    /* no more listening for now */
    close(servfd);

    if( clifd < 0 )
        perror( "Error: accept failed" );
    else
        print("now connected to client at %s", inet_ntoa(cliaddr.sin_addr));

    return clifd;
}

static int connectToServer(uint32_t server_ip, uint16_t port) {
    struct sockaddr_in servaddr;
    int fd;

    /* initialize the server's info */
    servaddr.sin_family = AF_INET;
    servaddr.sin_addr.s_addr = server_ip;
    servaddr.sin_port = htons(port);

    /* make a TCP socket for the new flow */
    if( (fd = socket( AF_INET, SOCK_STREAM, IPPROTO_TCP )) == -1 ) {
        perror("Error: unable to create socket to connect to remote server");
        exit(1);
    }

    /* connect to the server */
    print("Trying to connect to the remote server");
    if( connect( fd, (struct sockaddr*)&servaddr, sizeof(servaddr) ) != 0 ) {
        perror("Error: connect to remote server failed");
        close(fd);
        return -1;
    }
    else {
        print("Connected to the remote server");
        return fd;
    }
}

static int forwardData(int fdFrom, const char* strFrom, int fdTo, const char* strTo) {
#define BUF_SIZE 1500
    uint8_t buf[BUF_SIZE];
    int len;

    /* wait for data to forward */
    len = read(fdFrom, buf, BUF_SIZE);

    /* if an error occurred, determine if it was fatal */
    if(len < 0) {
        if( errno == EINTR )
            return 1;

        /* fatal error */
        print("error when trying to read from %s", strFrom);
        return 0;
    }

    /* watch for EOF */
    if(len == 0) {
        print("EOF received from %s", strFrom);
        return 1; /* other side may still send data */
    }

    /* pipe the data to the other fd */
    if( writen(fdTo, buf, len) != len ) {
        print("error when trying to forward data from %s to %s", strFrom, strTo);
        return 0;
    }

    return 1;
}

static void proxyLoop(uint32_t server_ip, uint16_t port) {
    int remote_client_fd, remote_server_fd;
    fd_set rdset, errset;
    int max_fd = -1, ret;

    /* get connected to the server first */
    remote_server_fd = connectToServer(server_ip, port);
    if( remote_server_fd < 0 )
        return;

    /* now wait for the client */
    remote_client_fd = waitForClient(port);
    if( remote_client_fd < 0 )
        return;

    /* determine the max file descriptor value */
    max_fd = (remote_client_fd > remote_server_fd) ? remote_client_fd : remote_server_fd;

    /* wait for data from one or the other and then forward it */
    while(1) {
        /* flag the descriptors we're interested in read and error events for */
        FD_ZERO(&rdset);
        FD_SET(remote_client_fd, &rdset);
        FD_SET(remote_server_fd, &rdset);

        FD_ZERO(&errset);
        FD_SET(remote_client_fd, &errset);
        FD_SET(remote_server_fd, &errset);

        /* wait for a read or error to occur */
        ret = select(max_fd+1, &rdset, NULL, &errset, NULL);

        /* handle the event */
        if( FD_ISSET(remote_client_fd, &errset) ) {
            print("remote client connection has encountered an error");
            return;
        }
        else if( FD_ISSET(remote_server_fd, &errset) ) {
            print("remote server connection has encountered an error");
            return;
        }
        else if( FD_ISSET(remote_client_fd, &rdset) ) {
            if( !forwardData(remote_client_fd, "remote client", remote_server_fd, "remote server") )
                return;
        }
        else if( FD_ISSET(remote_server_fd, &rdset) ) {
            if( !forwardData(remote_server_fd, "remote server", remote_client_fd, "remote client") )
                return;
        }
        else
            print("select returned with no flags set??");
    }
}

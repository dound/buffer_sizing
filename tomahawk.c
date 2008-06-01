/**
 * Filename: tomahawk.c
 * Purpose:  a very simple TCP flow generator
 */

#include <arpa/inet.h>
#include <errno.h>
#include <fcntl.h>
#include <pthread.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stropts.h>
#include <sys/socket.h>
#include <sys/time.h>
#include <time.h>
#include <unistd.h>
#include "common.h"
#include "debug.h"
#include "io_wrapper.h"

#define STR_VERSION "0.01b"

#define STR_USAGE "\
Tomahawk Flow Generator v%s\n\
%s: [-?]\n\
  -?, -help:       displays this help\n\
\n\
Client: -client CLIENT_PARAMETERS\n\
  -c, -client:     run tomahawk in client mode\n\
  -d, -dst, -ip:   sets the server IP address to connect to\n\
  -p, -port:       sets the server port to connect to\n\
  -n, -nflow:      the number of flows to establish\n\
  -bw, -bandwidth: the amount of bandwidth to use in bits per second (not including overheads)\n\
  -i, -interval:   interval in milliseconds at which a client will write\n\
  -l, -listen:     the port to listen on for updates to parameters\n\
\n\
Server: -server SERVER_PARAMETERS\n\
  -s, -server:    run tomahawk in server mode\n\
  -p, -port:      sets the port number to listen on\n\
  -n, -nap:       milliseconds to sleep between emptying buffers (default=50ms)\n"

#define MAX_FLOWS 65535
#define MAX_PAYLOAD (MAX_FLOWS * sizeof(int))
#define STATS_INTERVAL_SEC 1

/** encapsulates information about a client */
typedef struct {
    addr_ip_t server_ip;
    uint16_t  server_port;
    uint16_t  num_flows;
    uint32_t  bytes_per_sec;
    uint32_t  interval_millisec;
    uint16_t  listen_port;
    bool      warned;
} client_t;

/** encapsulates a message to a client's controller */
typedef struct {
    byte code;
    uint32_t val;
} __attribute__ ((packed)) control_t;

typedef enum {
    CODE_FLOWS,
    CODE_BPS,
    CODE_INTERVAL,
    CODE_EXIT
} code_t;

static void* controller_main( void* pclient );
static void client_main( client_t* c );
static void server_main( uint16_t port, uint32_t nap_usec );

/**
 * Convert bits to bytes, round up to nearest byte.
 */
static uint32_t bits_to_bytes_ceil( uint32_t num_bits ) {
    return ((num_bits - 1) / 8) + 1;
}

int main( int argc, char** argv ) {
    bool is_client = FALSE;
    bool is_server = FALSE;
    uint16_t port = 0;
    client_t client;
    uint32_t nap_usec = 50;

    /* initialize */
    debug_pthread_init_init();
    memset( &client, 0, sizeof(client) );

    /* ignore the broken pipe signal */
    signal( SIGPIPE, SIG_IGN );

    /* parse command-line arguments */
    unsigned i;
    for( i=1; i<argc || argc<=1; i++ ) {
        if( argc<=1 || str_matches(argv[i], 5, "-?", "-help", "--help", "help", "?") ) {
            printf( STR_USAGE, STR_VERSION, (argc>0) ? argv[0] : "tomahawk" );
            return 0;
        }
        else if( str_matches(argv[i], 3, "-c", "-client", "--client") ) {
            is_client = TRUE;
        }
        else if( str_matches(argv[i], 3, "-s", "-server", "--server") ) {
            is_server = TRUE;
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
            client.server_ip = in_ip.s_addr;
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
            port = val;
        }
        else if( str_matches(argv[i], 3, "-n", "-nflow", "--nflow") ) {
            i += 1;
            if( i == argc ) {
                fprintf( stderr, "Error: -nflow requires a number to be specified\n" );
                return -1;
            }
            uint32_t val = strtoul( argv[i], NULL, 10 );
            if( val==0 || val>MAX_FLOWS ) {
                fprintf( stderr, "Error: %u is not a valid number of flows\n", val );
                return -1;
            }
            client.num_flows = val;
        }
        else if( str_matches(argv[i], 5, "-b", "-bw", "--bw", "-bandwidth", "--bandwidth") ) {
            i += 1;
            if( i == argc ) {
                fprintf( stderr, "Error: -bandwidth requires a number to be specified\n" );
                return -1;
            }
            uint32_t val = strtoul( argv[i], NULL, 10 );
            if( val==0 ) {
                fprintf( stderr, "Error: %u is not a valid bandwidth rate\n", val );
                return -1;
            }
            client.bytes_per_sec = bits_to_bytes_ceil(val);
        }
        else if( str_matches(argv[i], 3, "-i", "-interval", "--interval") ) {
            i += 1;
            if( i == argc ) {
                fprintf( stderr, "Error: -interval requires a number to be specified\n" );
                return -1;
            }
            uint32_t val = strtoul( argv[i], NULL, 10 );
            if( val==0 ) {
                fprintf( stderr, "Error: %u is not a valid interval\n", val );
                return -1;
            }
            client.interval_millisec = val;
        }
        else if( str_matches(argv[i], 3, "-l", "-listen", "--listen") ) {
            i += 1;
            if( i == argc ) {
                fprintf( stderr, "Error: -listen requires a port number to be specified\n" );
                return -1;
            }
            uint32_t val = strtoul( argv[i], NULL, 10 );
            if( val==0 || val > 65535 ) {
                fprintf( stderr, "Error: %u is not a valid port to listen on\n", val );
                return -1;
            }
            client.listen_port = val;
        }
        else if( str_matches(argv[i], 3, "-n", "-nap", "--nap") ) {
            i += 1;
            if( i == argc ) {
                fprintf( stderr, "Error: -nap requires a number to be specified\n" );
                return -1;
            }
            uint32_t val = strtoul( argv[i], NULL, 10 );
            if( val==0 || val >= 1000 ) {
                fprintf( stderr, "Error: %u is not a valid nap quantity (must be in the range (0, 1000))\n", val );
                return -1;
            }
            nap_usec = val * 1000;
        }
        else {
            fprintf( stderr, "Error: unrecognized switch '%s'\n", argv[i] );
            return -1;
        }
    }
    if( port == 0 ) {
        fprintf( stderr, "Error: missing required switch -port\n" );
        return -1;
    }
    if( is_client ) {
        client.server_port = port;
        if( client.server_ip == 0 ) {
            fprintf( stderr, "Error: missing required switch -ip\n" );
            return -1;
        }
        if( client.num_flows == 0 ) {
            fprintf( stderr, "Error: missing required switch -nflow\n" );
            return -1;
        }
        if( client.bytes_per_sec == 0 ) {
            fprintf( stderr, "Error: missing required switch -bandwidth\n" );
            return -1;
        }
        if( client.interval_millisec == 0 ) {
            fprintf( stderr, "Error: missing required switch -interval\n" );
            return -1;
        }
        if( client.listen_port == 0 ) {
            fprintf( stderr, "Error: missing required switch -listen\n" );
            return -1;
        }
        client.warned = FALSE;

        /* start the background controller */
        pthread_t tid;
        if( 0 != pthread_create( &tid, NULL, controller_main, &client ) ) {
            fprintf( stderr, "Error: unable to start controller thread\n" );
            return -1;
        }

        /* start sending data to the server */
        client_main( &client );
    }
    else if( !is_server ) {
        fprintf( stderr, "Error: must specify either -client or -server\n" );
        return -1;
    }
    else {
        /* start listening for client connections */
        server_main( port, nap_usec );
    }

    return 0;
}

static struct timeval time_init;
static uint64_t total_bytes = 0;
static uint32_t total_packets = 0;
static uint32_t total_partial_writes = 0;
static uint32_t total_empty_writes = 0;
static void client_report_stats_and_exit( int code );
static void client_reset_checkpoint();

/** periodically sends stats to the master */
static void* controller_writer_main( void* vclientfd ) {
    int clientfd = (int)vclientfd;

    while( 1 ) {
        if( writen(clientfd, &total_bytes, sizeof(total_bytes)) != sizeof(total_bytes) ) {
            fprintf( stderr, "Error: controller failed to write stats to socket" );
            return NULL;
        }
        sleep( STATS_INTERVAL_SEC );
    }
}

/** listens for incoming connections from the master who will send commands */
static void* controller_main( void* pclient ) {
    client_t* c = (client_t*)pclient;
    struct sockaddr_in addr;
    struct sockaddr_in client_addr;
    unsigned sock_len = sizeof(client_addr);
    int sockfd;
    int clientfd;
    int len;
    struct sockaddr* p_ca = (struct sockaddr*)&client_addr;

    debug_pthread_init( "Controller", "Controller" );
    pthread_detach( pthread_self() );

    /* create a socket to listen for connections on */
    sockfd = socket( AF_INET, SOCK_STREAM, 0 );
    if( sockfd == -1 ) {
        printf( "Error: unable to create TCP socket for controller\n" );
        exit( 1 );
    }

    /* bind to the requested port */
    uint16_t port = c->listen_port;
    addr.sin_port = htons(port);
    addr.sin_addr.s_addr = 0;
    memset(&(addr.sin_zero), 0, sizeof(addr.sin_zero));
    if( bind(sockfd, (struct sockaddr*)&addr, sizeof(addr)) ) {
        printf( "Error: unable to bind to local port %u for controller\n", port );
        exit( 1 );
    }

    /* listen for incoming connection request */
    listen( sockfd, 1 );

    while( 1 ) {
        debug_println( "controller waiting for new connection on port %u", port );

        /* wait for a new client */
        clientfd = accept( sockfd, p_ca, &sock_len );
        if( clientfd == - 1 ) {
            if( errno == EINTR )
                continue;

            /* some error */
            fprintf( stderr, "Warrning: accept() failed (%d) for controller\n", errno );
            continue;
        }
        debug_println( "controller accepted a new connection\n" );

        /* start up the stats sending thread */
        pthread_t tid;
        if( 0 != pthread_create( &tid, NULL, controller_writer_main, (void*)clientfd ) ) {
            fprintf( stderr, "Error: unable to create controller writer thread\n" );
            client_report_stats_and_exit( 0 );
        }

        /* wait for control packets */
        control_t packet;
        while( (len=readn(clientfd, &packet, sizeof(packet))) ) {
            if( len < 0 )
                break;

            packet.val = ntohl( packet.val );
            if( packet.val == 0 && packet.code != CODE_EXIT ) {
                fprintf( stderr, "warning: was asked to change code=%u to zero", packet.code );
                continue;
            }

            switch( packet.code ) {
            case CODE_FLOWS:
                c->num_flows = packet.val;
                fprintf( stderr, "# of flows is now %u\n", packet.val );
                break;

            case CODE_BPS:
                c->bytes_per_sec = bits_to_bytes_ceil(packet.val);
                fprintf( stderr, "bps is now %ub\n", packet.val );
                break;

            case CODE_INTERVAL:
                c->interval_millisec = packet.val;
                fprintf( stderr, "interval is now %ums\n", packet.val );
                break;

            case CODE_EXIT:
                fprintf( stderr, "client exiting (received exit command)\n" );
                client_report_stats_and_exit( 0 );
                break;

            default:
                fprintf( stderr, "controller got unexpected packet code %u\n", packet.code );
            }
            c->warned = FALSE;
            client_reset_checkpoint();
        }

        close( clientfd );
        debug_println( "controller connection closed\n" );
    }

    close( sockfd );
    return NULL;
}

static void client_report_stats_and_exit( int code ) {
    struct timeval now;
    uint32_t time_passed_sec;
    uint32_t avg_bps;

    /* compute some stats before exiting */
    gettimeofday( &now, NULL );
    time_passed_sec = now.tv_sec - time_init.tv_sec;

    if( time_passed_sec )
        avg_bps = (total_bytes * 8) / time_passed_sec;
    else
        avg_bps = 0;

    /* report the stats */
    fprintf( stderr, "\
Client exiting (%d) ...\n\
  Packets Sent:     %10u\n\
  Partial Writes:   %10u\n\
  Empty Writes:     %10u\n\
  Bytes Sent:       %10llu\n\
  Total Uptime (s): %10u\n\
  Average BW (bps): %10u\n",
             code, total_packets, total_partial_writes, total_empty_writes, total_bytes, time_passed_sec, avg_bps );

    /* goodbye! */
    exit( code );
}

static void client_sig_int_handler( int sig ) {
    client_report_stats_and_exit( 0 );
}

static uint64_t checkpointed_prev = 0;
static uint64_t checkpointed_bytes;
static struct timeval checkpoint;
static void client_reset_checkpoint() {
    gettimeofday( &checkpoint, NULL );
    checkpointed_bytes = 0;
}

static void client_main( client_t* c ) {
    struct sockaddr_in servaddr;
    struct timeval start, end;
    int send_buf_size = 0, rcv_buf_size = 1024;
    uint32_t actual_flows = 0;
    uint32_t req_num_flows;
    bool pause_ok = FALSE;
    uint32_t interval_ms;
    uint32_t Bps;
    uint32_t num_bytes, extra_bytes = 0;
    uint32_t actual_bytes, expected_bytes, time_elapsed;
    int32_t time_ms, ret;
    bool skip_next_sleep = FALSE;
    uint32_t i;
    int fd[MAX_FLOWS];

    debug_pthread_init( "Client", "Client" );
    gettimeofday( &time_init, NULL );
    signal( SIGINT, client_sig_int_handler );

    /* initialize the server's info */
    servaddr.sin_family = AF_INET;
    servaddr.sin_addr.s_addr = c->server_ip;
    servaddr.sin_port = htons( c->server_port );
    client_reset_checkpoint();

    /* one loop per interval */
    while( 1 ) {
        /* note the time this interval starts */
        gettimeofday( &start, NULL );

        /* determine how many flows we're supposed to have now */
        req_num_flows = c->num_flows;
        interval_ms = c->interval_millisec;
        Bps = c->bytes_per_sec;

        /* create the requested number of flows */
        pause_ok = FALSE;
        while( actual_flows < req_num_flows ) {
            debug_println( "starting new flow (have %u, need %u)",
                           actual_flows, req_num_flows );

            /* make a TCP socket for the new flow */
            if( (fd[actual_flows] = socket( AF_INET, SOCK_STREAM, IPPROTO_TCP )) == -1 ) {
                perror( "Error: unable to create TCP socket for flow" );
                client_report_stats_and_exit( 1 );
            }

            /* connect to the server */
            if( connect( fd[actual_flows], (struct sockaddr*)&servaddr, sizeof(servaddr) ) != 0 ) {
                perror( "Error: connect for new flow failed" );
                client_report_stats_and_exit( 1 );
            }

            /* turn the socket into a non-blocking socket */
            if( fcntl(fd[actual_flows], F_SETFL, O_NONBLOCK) != 0 ) {
                perror( "Error: unable to put socket into non-blocking mode" );
                exit( 1 );
            }

            if( !send_buf_size ) {
                i = sizeof(int);
                if( -1==getsockopt(fd[actual_flows], SOL_SOCKET, SO_SNDBUF, &send_buf_size, &i) ) {
                    perror( "Error: unable to retrieve send buffer size" );
                    client_report_stats_and_exit( 1 );
                }
                fprintf( stderr, "Send Buffer Size = %dB\n", send_buf_size );
            }
            setsockopt(fd[actual_flows], SOL_SOCKET, SO_RCVBUF, &rcv_buf_size, sizeof(rcv_buf_size) );

            /* success! */
            actual_flows += 1;
            pause_ok = TRUE;
        }

        /* destroy any extra flows */
        while( actual_flows > req_num_flows ) {
            debug_println( "closing extraneous flow (have %u, need %u)",
                           actual_flows, req_num_flows );

            /* close the extra flow */
            actual_flows -= 1;
            close( fd[actual_flows] );
            pause_ok = TRUE;
        }

        /* compute how much traffic each client should send this interval */
        num_bytes = (extra_bytes + Bps * interval_ms) / (req_num_flows * 1000);
        if( num_bytes == 0 ) {
            if( !c->warned ) {
                fprintf( stderr, "Warning: payload bytes will only be 1\n" );
                c->warned = TRUE;
            }
            num_bytes = 1;
        }
        else if( num_bytes > MAX_PAYLOAD ) {
            if( !c->warned ) {
                fprintf( stderr, "Warning: payload bytes will be truncated to %u (should be %u)\n",
                               MAX_PAYLOAD, num_bytes );
                c->warned = TRUE;
            }
            num_bytes = MAX_PAYLOAD;
        }

        /* send garbage with each client */
        for( i=0; i<actual_flows; i++ ) {
            ret = writen( fd[i], &fd, num_bytes ); /* reuse fd buffer as "data" */
            if( ret == num_bytes ) {
                total_bytes += num_bytes;
                total_packets += 1;
            }
            else if( ret > 0 ) {
                total_bytes += ret;
                total_partial_writes += 1;
            }
            else if( ret == 0 )
                total_empty_writes += 1;
        }

        /* pause for the rest of the interval */
        gettimeofday( &end, NULL );
        time_ms = get_time_passed_ms( &start, &end );
        if( time_ms < (int64_t)interval_ms ) {
            if( !skip_next_sleep )
                sleep_ms( interval_ms - time_ms );
            else
                skip_next_sleep = FALSE;
        }
        else if( !pause_ok ) {
            extra_bytes = (Bps * (time_ms - interval_ms)) / 1000;
            fprintf( stderr, "Warning: interval took too long (took %ums; needed < %ums) (will try to catch up by sending %u extra bytes next interval)\n",
                     time_ms, interval_ms, extra_bytes );
        }
        else {
            fprintf( stderr, "Warning: interval took too long (took %ums; needed < %ums) (will not try to catch up because we spent part of the interval creating/destroying flows)\n",
                     time_ms, interval_ms );
        }

        /* what is the total elapsed time */
        gettimeofday( &end, NULL );
        time_elapsed = get_time_passed_ms( &checkpoint, &end );

        /* compute how many bytes were sent since the last checkpoint */
        actual_bytes = total_bytes - checkpointed_bytes - checkpointed_prev;

        /* were too many bytes sent? */
        expected_bytes = Bps * time_elapsed / 1000;
        if( actual_bytes > expected_bytes ) {
            /* determine how much extra time we need to spend sleeping */
            sleep_ms( (1000 * (actual_bytes - expected_bytes)) / Bps );
        }
        else if( (actual_bytes + Bps) < expected_bytes )
            skip_next_sleep = TRUE; /* skip sleep if we get a second behind */
    }
}

#define JUNK_SIZE 65536
static void server_main( uint16_t port, uint32_t nap_usec ) {
    struct sockaddr_in addr;
    struct sockaddr_in client_addr;
    unsigned sock_len = sizeof(client_addr);
    int sockfd;
    struct sockaddr* p_ca = (struct sockaddr*)&client_addr;
    struct timespec sleep_time;
    uint32_t fd_num = 0;
    uint32_t i;
    byte junk[JUNK_SIZE];
    int fd[MAX_FLOWS+1];

    debug_pthread_init( "Server", "Server" );
    sleep_time.tv_sec = 0;
    sleep_time.tv_nsec = nap_usec * 1000;

    /* create a socket to listen for connections on */
    sockfd = socket( AF_INET, SOCK_STREAM, 0 );
    if( sockfd == -1 ) {
        printf( "Error: unable to create TCP socket for server\n" );
        exit( 1 );
    }

    /* bind to the requested port */
    addr.sin_port = htons(port);
    addr.sin_addr.s_addr = 0;
    memset(&(addr.sin_zero), 0, sizeof(addr.sin_zero));
    if( bind(sockfd, (struct sockaddr*)&addr, sizeof(addr)) ) {
        printf( "Error: unable to bind to local port %u for server\n", port );
        exit( 1 );
    }

    /* turn the socket into a non-blocking socket */
    if( fcntl(fd[fd_num], F_SETFL, O_NONBLOCK) != 0 ) {
        perror( "Error: unable to put socket into non-blocking mode" );
        exit( 1 );
    }

    /* listen for incoming connection requests */
    listen( sockfd, SOMAXCONN );

    while( 1 ) {
        debug_println( "server waiting for new connection on port %u", port );

        /* accept new clients */
        while( (fd[fd_num]=accept(sockfd, p_ca, &sock_len )) != -1 ) {
            fd_num += 1;
            debug_println( "server has accepted a new client" );
            if( fd_num == MAX_FLOWS + 1 ) {
                fprintf( stderr, "Error: too many connection requests (%u)\n", fd_num );
                exit( 1 );
            }
        }
#ifdef _DEBUG_
        if( errno!=EINTR && errno!=EWOULDBLOCK ) {
            perror( "accept failed" );
            exit( 1 );
        }
#endif

        /* remove any existing data from the receive buffers */
        /* (socket is in discard mode so leftover data will be tossed) */
        for( i=0; i<fd_num; i++ )
            read( fd[i], junk, JUNK_SIZE ); /* don't care if it fails */

        nanosleep( &sleep_time, NULL );
    }

    close( sockfd );
}

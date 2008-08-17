/**
 * Filename: buf_size_daemon.c
 * Purpose: defines a client which continuously monitors the NetFPGA and reports
 *          its observations to a server.
 */

#ifdef _LINUX_
#include <stdint.h> /* uintX_t */
#endif
#include <arpa/inet.h>
#include <errno.h>
#include <pthread.h>
#include <signal.h>
#include <stdarg.h>
#include <sys/time.h>
#include <time.h>
#include <unistd.h>
#include "../common.h"
#include "../io_wrapper.h"
#include "nf2util.h"
#include "reg_defines_bsr.h"

/** SEND_STATS, if defined, will poll stats and send them to the server (don't use this and eventcap) */
/* #define SEND_STATS */

#define STR_VERSION "0.02b"

#define STR_USAGE "\
Buffer Size Reporting Daemon v%s\n\
%s: [-?]\n\
  -?, -help:       displays this help\n\
  -d, -dst, -ip:   sets the server IP address to connect to\n\
  -p, -port:       sets the server port to connect to (both UDP and TCP)\n"

/** number of seconds between updates */
#define UPDATE_INTERVAL_SEC 0
#define UPDATE_INTERVAL_NSEC (1000 * 1000) /* one per millisecond */

/** port to contact the server on */
#define DEFAULT_PORT 10272

/** encapsulates a message to a client's controller */
typedef struct {
#ifdef _LITTLE_ENDIAN_
    byte code:6;
    byte queue:2;
#else
    byte queue:2;
    byte code:6;
#endif
    uint32_t val;
} __attribute__ ((packed)) control_t;

typedef struct {
    uint32_t sec;
    uint32_t usec;
    uint32_t bytes_sent;
    uint32_t queue_occ;
} __attribute__ ((packed)) update_t;

typedef enum {
    CODE_SET_RATE_LIMIT=1,
    CODE_SET_BUF_SIZE=3
} code_t;

static nf2_device_t nf2;
static uint32_t server_ip;
static uint16_t server_port;

static void* controller_main( void* nil );
static void inform_server_loop();
static uint32_t get_rate_limit( int queue );
static void set_rate_limit( int queue, uint32_t shift );
static uint32_t get_bytes_sent();
static uint32_t get_buffer_size_packets( int queue );
static void set_buffer_size_packets( int queue, uint32_t size );
static uint32_t get_queue_occupancy_packets( int queue );

int main( int argc, char** argv ) {
    server_ip = 0;
    server_port = DEFAULT_PORT;

    /* ignore the broken pipe signal */
    signal( SIGPIPE, SIG_IGN );

    /* parse command-line arguments */
    unsigned i;
    for( i=1; i<argc || argc<=1; i++ ) {
        if( argc<=1 || str_matches(argv[i], 5, "-?", "-help", "--help", "help", "?") ) {
            printf( STR_USAGE, STR_VERSION, (argc>0) ? argv[0] : "buf_size_daemon" );
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
    }
    if( server_ip==0 ) {
        fprintf( stderr, "Error: -dst is a required argument; you must supply a server IP\n" );
        exit( 1 );
    }

    /* connect to the hardware */
    hw_init( &nf2 );

    /* listen for commands from the server */
    pthread_t tid;
    if( 0 != pthread_create( &tid, NULL, controller_main, NULL ) ) {
        fprintf( stderr, "Error: unable to start controller thread\n" );
        return -1;
    }

    /* tell the server what is going on from time to time */
#if SEND_STATS
    inform_server_loop();
#endif

    /* cleanup */
    closeDescriptor( &nf2 );
    return 0;
}

static void rc_print( const char* format, ... ) {
#ifdef _DEBUG_
    va_list args;
    va_start( args, format );

    fprintf( stderr, "[Router Controller Server] " );
    vfprintf( stderr, format, args );
    fprintf( stderr, "\n" );

    va_end( args );
#endif
}

/** listens for incoming connections from the master who will send commands */
static void* controller_main( void* nil ) {
    struct sockaddr_in servaddr;
    struct sockaddr_in cliaddr;
    socklen_t cliaddr_len;
    int servfd, clifd, len;

    /* initialize the server's info */
    servaddr.sin_family = AF_INET;
    servaddr.sin_addr.s_addr = htonl(INADDR_ANY);
    servaddr.sin_port = htons( server_port );

    /* make a TCP socket for the new flow */
    if( (servfd = socket( AF_INET, SOCK_STREAM, IPPROTO_UDP )) == -1 ) {
        perror( "Error: unable to create UDP socket for controller" );
        exit( 1 );
    }

    /* listen for incoming connections */
    rc_print("listening for an incoming connection request to UDP port %u", server_port);
    if( listen(servfd, 1) < 0 ) {
        perror( "Error: unable to listen" );
        exit( 1 );
    }

    /* loop forever */
    while( 1 ) {
        rc_print("waiting for client to connect ...");
        if( (clifd=accept(servfd, (struct sockaddr*)&cliaddr, &cliaddr_len)) < 0 ) {
            perror( "Error: accept failed" );
            continue;
        }
        rc_print("now connected to client at %s", inet_ntoa(cliaddr.sin_addr));

        /* wait for control packets */
        control_t packet;
        while( (len=readn(clifd, &packet, sizeof(packet))) ) {
            if( len < 0 ) {
                rc_print("received EOF from client");
                break;
            }

            packet.val = ntohl( packet.val );
            switch( packet.code ) {
            case CODE_SET_RATE_LIMIT:
                set_rate_limit( packet.queue, packet.val );
                break;

            case CODE_SET_BUF_SIZE:
                set_buffer_size_packets( packet.queue, packet.val );
                break;

            default:
                rc_print("received unexpected packet code %u", packet.code);
            }
        }

        rc_print("connection to client at %s closed", inet_ntoa(cliaddr.sin_addr));
        close(clifd);
    }

    close(servfd);
    return NULL;
}

/** Periodically sends an update with the NetFPGA's info to the master. */
static void inform_server_loop() {
    int fd;
    struct timespec sleep_time;
    sleep_time.tv_sec = UPDATE_INTERVAL_SEC;
    sleep_time.tv_nsec = UPDATE_INTERVAL_NSEC;

    /* make a UDP socket to send data to the server on */
    if( (fd = socket( AF_INET, SOCK_DGRAM, IPPROTO_TCP )) == -1 ) {
        perror( "Error: unable to create TCP socket" );
        exit( 1 );
    }

    /* allow reuse of the port */
    int reuse = 1;
    if( setsockopt( fd, SOL_SOCKET, SO_REUSEADDR, &reuse, sizeof(reuse) ) ) {
        perror( "Error: SO_REUSEADDR failed" );
        exit( 1 );
    }

    /* bind to the socket */
    struct sockaddr_in sin;
    sin.sin_family = AF_INET;
    sin.sin_port = htons( server_port );
    sin.sin_addr.s_addr = INADDR_ANY;
    if( bind( fd, (struct sockaddr *)&sin, sizeof(sin) ) == -1 ) {
        perror( "Error: bind failed to UDP SR Monitor Port" );
        exit( 1 );
    }

    struct timeval now;
    update_t update;
    while( 1 ) {
        /* create an update */
        gettimeofday( &now, NULL );
        update.sec        = htonl( now.tv_sec );
        update.usec       = htonl( now.tv_usec );
        update.bytes_sent = htonl( get_bytes_sent() );
        update.queue_occ  = htonl( get_queue_occupancy_packets(1) );
        /* note: still have 2B before we hit min Eth payload (incl overheads) */

        /* reuse sin as server addr */
        sin.sin_addr.s_addr = server_ip;

        /* send the update to the server */
        if( -1 == sendto( fd, (char*)&update, sizeof(update), 0,
                          (struct sockaddr*)&sin, sizeof(sin) ) ) {
            if( errno != EINTR ) {
                perror( "Error: sendto failed" );
                exit( 1 );
            }
        }

        /* wait a while before compiling the next packet */
        nanosleep( &sleep_time, NULL );
    }
}

static uint32_t get_rate_limit( int queue ) {
    uint32_t enabled, val;

    if( queue != 1 ) {
        fprintf( stderr, "Error: only queue 1 (not queue %d) can do rate limiting\n", queue );
        exit( 1 );
    }

    readReg( &nf2, RATE_LIMIT_ENABLE_REG, &enabled );
    readReg( &nf2, RATE_LIMIT_SHIFT_REG, &val );
    return enabled ? val : 0;
}

static void set_rate_limit( int queue, uint32_t shift ) {
    if( queue != 1 ) {
        fprintf( stderr, "Error: only queue 1 (not queue %d) can do rate limiting\n", queue );
        exit( 1 );
    }

    uint32_t enabled = shift ? 1 : 0;
    writeReg( &nf2, RATE_LIMIT_ENABLE_REG, enabled );
    writeReg( &nf2, RATE_LIMIT_SHIFT_REG, shift );

    if( shift )
        rc_print("rate limiter has been changed to %u", shift);
    else
        rc_print("rate limiter has been disabled");
}

static uint32_t get_bytes_sent( int queue ) {
    unsigned reg;
    switch( queue ) {
    case 0: reg = OQ_NUM_PKT_BYTES_REMOVED_REG_0; break;
    case 1: reg = OQ_NUM_PKT_BYTES_REMOVED_REG_2; break;
    case 2: reg = OQ_NUM_PKT_BYTES_REMOVED_REG_4; break;
    case 3: reg = OQ_NUM_PKT_BYTES_REMOVED_REG_6; break;
    }

    uint32_t val;
    readReg( &nf2, reg, &val );
    return val;
}

static inline unsigned get_buffer_size_packets_reg( int queue ) {
    unsigned reg;
    switch( queue ) {
    case 0: reg = OQ_MAX_PKTS_IN_Q_REG_0; break;
    case 1: reg = OQ_MAX_PKTS_IN_Q_REG_2; break;
    case 2: reg = OQ_MAX_PKTS_IN_Q_REG_4; break;
    case 3: reg = OQ_MAX_PKTS_IN_Q_REG_6; break;
    }
    return reg;
}

static uint32_t get_buffer_size_packets( int queue ) {
    uint32_t val;
    readReg( &nf2, get_buffer_size_packets_reg(queue), &val );
    return val;
}

static void set_buffer_size_packets( int queue, uint32_t size ) {
    writeReg( &nf2, get_buffer_size_packets_reg(queue), size );
    rc_print("buffer size has been changed to %u packets in size", size);
}

static uint32_t get_queue_occupancy_packets( int queue ) {
    unsigned reg;
    switch( queue ) {
    case 0: reg = OQ_NUM_PKTS_IN_Q_REG_0; break;
    case 1: reg = OQ_NUM_PKTS_IN_Q_REG_2; break;
    case 2: reg = OQ_NUM_PKTS_IN_Q_REG_4; break;
    case 3: reg = OQ_NUM_PKTS_IN_Q_REG_6; break;
    }

    uint32_t val;
    readReg( &nf2, reg, &val );
    return val;
}

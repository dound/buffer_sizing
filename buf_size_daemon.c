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
#include <sys/time.h>
#include <time.h>
#include <unistd.h>
#include "common.h"
#include "io_wrapper.h"
#include "nf2util.h"
#include "reg_defines_bsr.h"

#define STR_VERSION "0.01b"

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
#define DEFAULT_PORT 60308

/** encapsulates a message to a client's controller */
typedef struct {
    byte code;
    uint32_t val;
} __attribute__ ((packed)) control_t;

typedef struct {
    uint32_t sec;
    uint32_t usec;
    uint32_t bytes_sent;
    uint32_t queue_occ;
} __attribute__ ((packed)) update_t;

typedef enum {
    CODE_GET_RATE_LIMIT=0,
    CODE_SET_RATE_LIMIT=1,
    CODE_GET_BUF_SIZE=2,
    CODE_SET_BUF_SIZE=3
} code_t;

static nf2_device_t nf2;
static uint32_t server_ip;
static uint16_t server_port;

static void* controller_main( void* nil );
static void inform_server_loop();
static uint32_t get_rate_limit();
static void set_rate_limit( uint32_t shift );
static uint32_t get_bytes_sent();
static uint32_t get_buffer_size();
static void set_buffer_size( uint32_t size );
static uint32_t get_queue_occupancy();

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
    inform_server_loop();

    /* cleanup */
    closeDescriptor( &nf2 );
    return 0;
}

/** listens for incoming connections from the master who will send commands */
static void* controller_main( void* nil ) {
    struct sockaddr_in servaddr;
    int fd, len;

    /* initialize the server's info */
    servaddr.sin_family = AF_INET;
    servaddr.sin_addr.s_addr = server_ip;
    servaddr.sin_port = htons( server_port );

    /* make a TCP socket for the new flow */
    if( (fd = socket( AF_INET, SOCK_STREAM, IPPROTO_TCP )) == -1 ) {
        perror( "Error: unable to create TCP socket for controller" );
        exit( 1 );
    }

    /* connect to the server */
    if( connect( fd, (struct sockaddr*)&servaddr, sizeof(servaddr) ) != 0 ) {
        perror( "Error: connect for controller failed" );
        exit( 1 );
    }

    while( 1 ) {
        fprintf( stderr, "Connected to the master\n" );

        /* wait for control packets */
        control_t packet;
        while( (len=readn(fd, &packet, sizeof(packet))) ) {
            if( len < 0 )
                break;

            int ret = 0;
            packet.val = ntohl( packet.val );
            switch( packet.code ) {
            case CODE_GET_RATE_LIMIT:
                packet.val = htonl( get_rate_limit() );
                ret = writen( fd, &packet.val, sizeof(packet.val) );
                break;

            case CODE_SET_RATE_LIMIT:
                set_rate_limit( packet.val );
                break;

            case CODE_GET_BUF_SIZE:
                packet.val = htonl( get_buffer_size() );
                ret = writen( fd, &packet.val, sizeof(packet.val) );
                break;

            case CODE_SET_BUF_SIZE:
                set_buffer_size( packet.val );
                break;

            default:
                fprintf( stderr, "controller got unexpected packet code %u\n", packet.code );
            }
            if( ret == -1 ) {
                fprintf( stderr, "controller failed to write (goodbye)\n" );
                exit( 1 );
            }
        }

        close( fd );
        fprintf( stderr, "master connection closed (goodbye)\n" );
        exit( 0 );
    }

    return NULL;
}

/** Periodically sends an update with the NetFPGA's info to the master. */
static void inform_server_loop() {
    int fd;
    struct timespec sleep_time;
    sleep_time.tv_sec = UPDATE_INTERVAL_SEC;
    sleep_time.tv_nsec = UPDATE_INTERVAL_NSEC;

    /* make a UDP socket to send data to the server on */
    if( (fd = socket( AF_INET, SOCK_DGRAM, IPPROTO_UDP )) == -1 ) {
        perror( "Error: unable to create UDP socket" );
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
        update.queue_occ  = htonl( get_queue_occupancy() );
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

static uint32_t get_rate_limit() {
    uint32_t enabled, val;
    readReg( &nf2, RATE_LIMIT_ENABLE_REG, &enabled );
    readReg( &nf2, RATE_LIMIT_SHIFT_REG, &val );
    return enabled ? val : 0;
}

static void set_rate_limit( uint32_t shift ) {
    uint32_t enabled = shift ? 1 : 0;
    writeReg( &nf2, RATE_LIMIT_ENABLE_REG, enabled );
    writeReg( &nf2, RATE_LIMIT_SHIFT_REG, shift );
}

static uint32_t get_bytes_sent() {
    /* reg 2 => nf2c1 */
    uint32_t val;
    readReg( &nf2, OQ_NUM_PKT_BYTES_REMOVED_REG_2, &val );
    return val;
}

static uint32_t get_buffer_size() {
    /* reg 2 => nf2c1 */
    uint32_t val;
    readReg( &nf2, OQ_MAX_PKTS_IN_Q_REG_2, &val );
    return val;
}

static void set_buffer_size( uint32_t size ) {
    /* reg 2 => nf2c1 */
    writeReg( &nf2, OQ_MAX_PKTS_IN_Q_REG_2, size );
}

static uint32_t get_queue_occupancy() { /* in 8B words */
    /* queue 1 => nf2c1 */
    uint32_t val;
    readReg( &nf2, OQ_NUM_WORDS_IN_Q_REG_2, &val );
    return val;
}

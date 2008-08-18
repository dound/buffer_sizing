/**
 * Filename: router_controller.c
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
#include <string.h>
#include <sys/time.h>
#include <time.h>
#include <unistd.h>
#include "../common.h"
#include "../debug.h"
#include "../io_wrapper.h"
#include "nf2helpers.h"
#include "nf2util.h"

#define STR_VERSION "0.02b"

#define STR_USAGE "\
Router Controller Server v%s\n\
  -?, -help:       displays this help\n\
  -l, -listen:     the port to listen for connections on\n\
  -e, -evcap:      the port to listen for event capture packets on\n\
  -c, -coallesce:  number of event capture packets to coallesce per info field\n\
  -n, -num:        number of info fields to (try to) send per packet\n\
  -b, -bw:         compute the estimated bandwidth and update rate and then exit\n\
  -v, -verbose:    verbosely print information to standard out\n\
  -vv, -vverbose:  very verbosely print information to standard out\n"

#define STR_PARAMS "\n\
    Command Listen TCP Port = %u\n\
    Event Capture Listen UDP Port = %u\n\
    Number of Event Capture Packets Per Info = %u\n\
    Number of Infos Per Update = %u\n\
    Approximate GUI Update Rate = %.1f updates per sec\n\
    Approximate GUI Update Bandwidth = %.1f kbps\n"

/** encapsulates a message to a client's controller */
typedef struct {
    byte code;
    byte queue;
    uint32_t val;
} __attribute__ ((packed)) control_t;

typedef struct {
    uint32_t sec;
    uint32_t usec;
    uint32_t arrived;
    uint32_t departed;
    uint32_t current;
} __attribute__ ((packed)) update_info_t;

#define MAX_FRAME_SIZE 1514
#define OVERHEAD_SIZE (14 + 20 + 32) /* 14B Ethernet header, 20B IP header, 20B
                                        TCP header, 12B TCP options seem typical */
#define MAX_PAYLOAD (MAX_FRAME_SIZE - OVERHEAD_SIZE)
#define MAX_UPDATE_INFOS (MAX_PAYLOAD / sizeof(update_info_t))

typedef enum {
    CODE_SET_RATE_LIMIT=1,
    CODE_SET_BUF_SIZE=3
} code_t;

#define EVENT_CAP_PORT 27033

static uint16_t server_port;
static uint16_t evcap_port;
static unsigned update_evcaps_per_info;
static unsigned update_infos_per_update_packet;
static int verbose = 0;
static double startTime;
static int client_fd = -1;

static void* controller_main( void* nil );
static void event_capture_handler();
static void parseEvCap(uint8_t* buf, unsigned len, update_info_t* u);

static void rc_print_timestamp() {
    struct timeval now;
    double t;

    gettimeofday(&now,NULL);
    t = now.tv_sec + now.tv_usec / 1000000.0 - startTime;

    fprintf( stdout, "%.3f: ", t );
}

static void rc_print( const char* format, ... ) {
    va_list args;
    va_start( args, format );

    rc_print_timestamp();
    fprintf( stdout, "[Router Controller Server] " );
    vfprintf( stdout, format, args );
    fprintf( stdout, "\n" );

    va_end( args );
}

static void rc_print_verbose( int level, const char* format, ... ) {
    va_list args;

    if( verbose < level )
        return;

    va_start( args, format );

    rc_print_timestamp();
    fprintf( stdout, "[Router Controller Server] " );
    vfprintf( stdout, format, args );
    fprintf( stdout, "\n" );

    va_end( args );
}

int main( int argc, char** argv ) {
    struct timeval now;

    /* initialize the start time */
    gettimeofday(&now, NULL);
    startTime = now.tv_sec + now.tv_usec / 1000000.0;

    /* default values for command-line parameters */
    int printOnly = 0;
    server_port = 10272;
    evcap_port = 27033;
    update_evcaps_per_info = 1;
    update_infos_per_update_packet = 10;

    /* ignore the broken pipe signal */
    signal( SIGPIPE, SIG_IGN );

    /* parse command-line arguments */
    unsigned i;
    for( i=1; i<argc; i++ ) {
        if( str_matches(argv[i], 5, "-?", "-help", "--help", "help", "?") ) {
            printf( STR_USAGE, STR_VERSION );
            return 0;
        }
        else if( str_matches(argv[i], 3, "-l", "-listen", "--listen") ) {
            i += 1;
            if( i == argc ) {
                rc_print("Error: -listen requires a port number to be specified");
                return -1;
            }
            uint32_t val = strtoul( argv[i], NULL, 10 );
            if( val==0 || val > 65535 ) {
                rc_print("Error: %u is not a valid port", val);
                return -1;
            }
            server_port = val;
        }
        else if( str_matches(argv[i], 3, "-e", "-evcap", "--evcap") ) {
            i += 1;
            if( i == argc ) {
                rc_print("Error: -evcap requires a port number to be specified");
                return -1;
            }
            uint32_t val = strtoul( argv[i], NULL, 10 );
            if( val==0 || val > 65535 ) {
                rc_print("Error: %u is not a valid port", val);
                return -1;
            }
            evcap_port = val;
        }
        else if( str_matches(argv[i], 3, "-c", "-coallesce", "--coallesce") ) {
            i += 1;
            if( i == argc ) {
                rc_print("Error: -coallesce requires an argument to be specified");
                return -1;
            }
            update_evcaps_per_info = strtoul( argv[i], NULL, 10 );
            if( update_evcaps_per_info == 0 ) {
                rc_print("Error: -coallesce must be greater than 0");
                return -1;
            }
        }
        else if( str_matches(argv[i], 3, "-n", "-num", "--num") ) {
            i += 1;
            if( i == argc ) {
                rc_print("Error: -num requires an argument to be specified");
                return -1;
            }
            update_infos_per_update_packet = strtoul( argv[i], NULL, 10 );
            if( update_infos_per_update_packet == 0 ) {
                rc_print("Error: -num must be greater than 0");
                return -1;
            }
            if( update_infos_per_update_packet > MAX_UPDATE_INFOS ) {
                rc_print("Error: -num must be no greater than %u (this is the most that fit in a single packet)", MAX_UPDATE_INFOS);
                return -1;
            }
        }
        else if( str_matches(argv[i], 3, "-b", "-bw", "--bw") ) {
            printOnly = 1;
        }
        else if( str_matches(argv[i], 3, "-v", "-verbose", "--verbose") ) {
            verbose = 1;
        }
        else if( str_matches(argv[i], 3, "-vv", "-vverbose", "--vverbose") ) {
            verbose = 2;
        }
    }

    /* compute the BW the update thread will consume between here and the gui */
    double evcaps_per_sec = 32.508; /* empircal measurement */
    double infos_per_sec = evcaps_per_sec / update_evcaps_per_info;
    double updates_per_sec = infos_per_sec / update_infos_per_update_packet;
    unsigned overhead_bytes = 2 * OVERHEAD_SIZE; /* 2x accounts for ACK too */
    unsigned update_size_bits = 8 * (overhead_bytes + update_infos_per_update_packet * sizeof(update_info_t));
    double rate_bps = updates_per_sec * update_size_bits;

    /* print and exit if that is all the user wanted */
    if( printOnly ) {
        printf( "%u\t%u\t%.1f\t%.1f\n", update_evcaps_per_info, update_infos_per_update_packet, updates_per_sec, rate_bps / 1000 );
        return 0;
    }

    rc_print(STR_PARAMS,
             server_port, evcap_port, update_evcaps_per_info, update_infos_per_update_packet,
             updates_per_sec, rate_bps / 1000);


    /* connect to the hardware */
    hw_init( &nf2 );

    /* listen for commands from the server */
    pthread_t tid;
    if( 0 != pthread_create( &tid, NULL, controller_main, NULL ) ) {
        rc_print("Error: unable to start the main controller thread");
        return -1;
    }

    /* listen for event capture traffic and tell the client about it */
    event_capture_handler();

    /* cleanup */
    closeDescriptor( &nf2 );
    return 0;
}

/** listens for incoming connections from the master who will send commands */
static void* controller_main( void* nil ) {
    struct sockaddr_in servaddr;
    struct sockaddr_in cliaddr;
    socklen_t cliaddr_len;
    int servfd, len, val;

    /* initialize the server's info */
    servaddr.sin_family = AF_INET;
    servaddr.sin_addr.s_addr = htonl(INADDR_ANY);
    servaddr.sin_port = htons( server_port );

    /* make a TCP socket for the new flow */
    if( (servfd = socket( AF_INET, SOCK_STREAM, IPPROTO_TCP )) == -1 ) {
        perror( "Error: unable to create TCP socket for router controller" );
        exit( 1 );
    }

    /* permit the socket to reuse the port even if it is already in use */
    val = 1;
    setsockopt(servfd, SOL_SOCKET, SO_REUSEADDR, &val, sizeof(val));

    /* bind to the port */
    if( bind(servfd, (struct sockaddr *)&servaddr, sizeof(servaddr)) < 0 ) {
        perror( "Error: unable to bind TCP socket for router controller" );
        exit(1);
    }

    /* listen for incoming connections */
    rc_print("listening for an incoming connection request to TCP port %u", server_port);
    if( listen(servfd, 1) < 0 ) {
        perror( "Error: unable to listen" );
        exit( 1 );
    }

    /* loop forever */
    while( 1 ) {
        rc_print("waiting for client to connect ...");
        cliaddr_len = sizeof(cliaddr);
        if( (client_fd=accept(servfd, (struct sockaddr*)&cliaddr, &cliaddr_len)) < 0 ) {
            perror( "Error: accept failed" );
            continue;
        }
        rc_print("now connected to client at %s", inet_ntoa(cliaddr.sin_addr));

        /* wait for control packets */
        control_t packet;
        while( (len=readn(client_fd, &packet, sizeof(packet))) ) {
            if( len < 0 ) {
                rc_print("received EOF from client");
                break;
            }

            packet.val = ntohl( packet.val );
            switch( packet.code ) {
            case CODE_SET_RATE_LIMIT:
                set_rate_limit( packet.queue, packet.val );

                if( packet.val )
                    rc_print("rate limiter has been changed to %u", packet.val);
                else
                    rc_print("rate limiter has been disabled");

                break;

            case CODE_SET_BUF_SIZE:
                set_buffer_size_packets( packet.queue, packet.val );
                rc_print("buffer size has been changed to %u packets in size", packet.val);
                break;

            default:
                rc_print("received unexpected packet code %u", packet.code);
            }
        }

        rc_print("connection to client at %s closed", inet_ntoa(cliaddr.sin_addr));
        close(client_fd);
        client_fd = -1;
    }

    close(servfd);
    return NULL;
}

#define TYPE_TS 0
#define TYPE_ARRIVE 1
#define TYPE_DEPART 2
#define TYPE_DROP 3

static void event_capture_handler() {
    update_info_t update[MAX_UPDATE_INFOS];
    unsigned updateInfoOn = 0;
    struct timeval now;
    struct sockaddr_in si_me, si_other;
    int evcap_fd, len, val;
    unsigned slen=sizeof(si_other), evcap_on;
    uint8_t buf[1500];

    if( (evcap_fd=socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP) ) == -1 ) {
        perror( "Error: unable to create UDP socket for event capture receiver" );
        exit( 1 );
    }

    memset((char *) &si_me, 0, sizeof(si_me));
    si_me.sin_family = AF_INET;
    si_me.sin_port = htons(EVENT_CAP_PORT);
    si_me.sin_addr.s_addr = htonl(INADDR_ANY);

    /* permit the socket to reuse the port even if it is already in use */
    val = 1;
    setsockopt(evcap_fd, SOL_SOCKET, SO_REUSEADDR, &val, sizeof(val));

    if( bind(evcap_fd, (struct sockaddr*)&si_me, sizeof(si_me)) == -1 ) {
        perror( "Error: unable to bind to UDP socket" );
        exit( 1 );
    }

    /* wait for event capture packets forever */
    evcap_on = 0;
    while( 1 ) {
        /* clear the update info if we just got to it */
        if( evcap_on++ == 0 )
            memset( &update[updateInfoOn], 0, sizeof(update_info_t) );

        /* get the packet */
        if( (len=recvfrom(evcap_fd, buf, 1500, 0, (struct sockaddr*)&si_other, &slen)) < 0 ) {
            perror( "Error: recvfrom failed to retrieve event capture packet" );
            exit( 1 );
        }

        /* parse the arrivals and departures */
        parseEvCap(buf, len, &update[updateInfoOn]);

        /* see if we've aggregated enough evcaps to finish this update info */
        if( evcap_on == update_evcaps_per_info ) {
            update_info_t *u;
            u = &update[updateInfoOn];

            /* note what time this update finished */
            gettimeofday( &now, NULL );
            u->sec  = now.tv_sec - (int)startTime;
            u->usec = now.tv_usec;

            /* print the update info */
            rc_print_verbose( 1,
                              "update info %u ready:\n    when: %usec:%uusec\n    arrived: %u\n    departed: %u\n    current: %u\n",
                              updateInfoOn,
                              u->sec,
                              u->usec,
                              u->arrived,
                              u->departed,
                              u->current );

            /* convert the update's fields from host to network order */
            u->sec      = htonl(u->sec);
            u->usec     = htonl(u->usec);
            u->arrived  = htonl(u->arrived);
            u->departed = htonl(u->departed);
            u->current  = htonl(u->current);

            /* on to the next update */
            updateInfoOn += 1;

            /* see if the packet is full yet */
            if( updateInfoOn == update_infos_per_update_packet ) {
                /* send the update to the GUI */
                if( client_fd >= 0 ) {
                    writen(client_fd, &update, update_infos_per_update_packet * sizeof(update_info_t));
                    rc_print_verbose(2, "update sent to client");
                }

                /* start again! */
                updateInfoOn = 0;
            }

            /* done with this aggregation of evcaps */
            evcap_on = 0;
        }
    }

    close(evcap_fd);
}

static uint32_t getU32(uint8_t* buf, unsigned index) {
    return ntohl(*((uint32_t*)&buf[index]));
}

#define ntohll(x) (((uint64_t)(ntohl((int)((x << 32) >> 32))) << 32) | (uint32_t)ntohl(((int)(x >> 32))))
static uint64_t getU64(uint8_t* buf, unsigned index) {
    return ntohll(*((uint64_t*)&buf[index]));
}
#define htonll(x) (ntohll(x))

#define DEFAULT_QUEUE_TO_MONITOR 2
#define USE_PACKETS 0
#define NUM_QUEUES 8
#define MASK_TYPE (0xC0000000)
#define MASK_QID  (0x38000000)
#define MASK_PLEN (0x07F80000)
#define MASK_TSA1 (0xFFFFFFFFFFF80000LL)
#define MASK_TSA2 (0x0007FFFF)

static void parseEvCap(uint8_t* buf, unsigned len, update_info_t* u) {
    int i, index, num_events, num_bytes, num_packets, type;
    uint64_t timestamp_8ns, timestamp_adjusted_8ns;
    static uint64_t lastTS = 0;

    if( len < 78 ) {
        rc_print( "Ignoring evcap packet which is too small (%uB)", len );
        return;
    }

    /* start processing at byte 1 (byte 0 isn't too interesting) */
    index = 1;
    num_events = buf[index];
    index += 1;

    /* skip the sequence number */
    rc_print_verbose( 2, "seq = %u", getU32(buf, index) );
    index += 4;

    /* get the timestamp before the queue data */
    timestamp_8ns = getU64( buf, 70 );
    if( lastTS > timestamp_8ns ) {
        rc_print( "old timestamp (ignoring) (received %llu, latest is %llu)", timestamp_8ns, lastTS );
        return; /* old, out-of-order packet */
    }
    else {
        rc_print_verbose( 2, "got new timestamp %llu", timestamp_8ns );
        lastTS = timestamp_8ns;
    }

    /* get queue occupancy data */
    for( i=0; i<NUM_QUEUES; i++ ) {
        /* update the queue with its new absolute value */
        if( !USE_PACKETS && i == DEFAULT_QUEUE_TO_MONITOR ) { /* only handle NF2C1 for now */
            num_bytes = 8 * getU32(buf, index);
            u->current = num_bytes;
            rc_print_verbose( 2, "queue 2 set to %uB", num_bytes );
        }
        index += 4;

        /* size in packets */
        if( USE_PACKETS && i == DEFAULT_QUEUE_TO_MONITOR ) { /* only handle NF2C1 for now */
            num_packets = getU32(buf, index);
            u->current = num_packets;
            rc_print_verbose( 2, "queue 2 set to %u packets" + num_packets );
        }
        index += 4;
    }

    /* already got the timestamp; keep going */
    index += 8;

    /* process each event */
    timestamp_adjusted_8ns = timestamp_8ns;
    while( index + 4 < len ) {
        type = (getU32(buf, index) & MASK_TYPE) >> 30;
        rc_print_verbose( 2, "  got type = 0x%0X", type );

        if( type == TYPE_TS ) {
            if( index + 8 >= len ) break;

            timestamp_8ns = getU64( buf, index );
            index += 8;
            rc_print_verbose( 2, "    got timestamp %llu", timestamp_8ns );
        }
        else {
            int val, queue_id, plen_bytes;

            /* determine the # of bytes involved and the offset */
            val = getU32( buf, index );
            queue_id = (val & MASK_QID) >> 27;
            plen_bytes = ((val & MASK_PLEN) >> 19) * 8 - 8; /* - 8 to not include NetFPGA overhead */
            timestamp_adjusted_8ns = (timestamp_8ns & MASK_TSA1) | ntohl(val & MASK_TSA2);
            index += 4;

            rc_print_verbose( 2, "    %uB %s for queue %u at timestamp %llu",
                           plen_bytes,
                           (type==TYPE_ARRIVE)?"arrived":(type==TYPE_DEPART)?"departed":"dropped",
                           queue_id,
                           timestamp_adjusted_8ns );

            /* only pay attention to NF2C1 for now */
            if( queue_id != DEFAULT_QUEUE_TO_MONITOR ) {
                rc_print_verbose( 2, "    ignoring event for queue %u", queue_id );
                continue;
            }

            if( type == TYPE_ARRIVE ) {
                if( USE_PACKETS )
                    u->arrived += 1;
                else
                    u->arrived += plen_bytes;

                rc_print_verbose( 2, "arrival => %u", u->arrived );
            }
            else if( type == TYPE_DEPART ) {
                if( USE_PACKETS )
                    u->departed += 1;
                else
                    u->departed += plen_bytes;

                rc_print_verbose( 2, "departure => %u", u->departed );
            }
            else
                rc_print_verbose( 2, "    (dropped)" );
        }
    }
}

/* Filename: common.c */

#include <arpa/inet.h>
#include <stdarg.h>
#include <stdio.h>      /* snprintf() */
#include <stdlib.h>     /* calloc, malloc, realloc */
#include <string.h>
#include <sys/time.h>   /* struct timeval */
#include <time.h>
#include "common.h"
#include "debug.h"

uint32_t get_time_passed_ms( struct timeval* start, struct timeval* end ) {
    uint32_t start_ms;
    uint32_t end_ms;

    start_ms = start->tv_sec*1000 + start->tv_usec / 1000;
    end_ms   =   end->tv_sec*1000 +   end->tv_usec / 1000;
    return( end_ms - start_ms );
}

void sleep_ms( uint32_t sleep_ms ) {
    struct timespec sleep_time;

    if( sleep_ms < 1000 )
        sleep_time.tv_sec = 0;
    else {
        sleep_time.tv_sec = sleep_ms / 1000;
        sleep_ms -= (sleep_time.tv_sec * 1000);
    }
    sleep_time.tv_nsec = sleep_ms * 1000 * 1000;
    nanosleep( &sleep_time, NULL );
}

void ip_to_string( char* buf, addr_ip_t ip ) {
    byte* bytes = (byte*)&ip;

    snprintf( buf, STRLEN_IP, "%u.%u.%u.%u",
              bytes[0],
              bytes[1],
              bytes[2],
              bytes[3] );
}

unsigned my_snprintf( char* str, size_t size, const char* format, ... ) {
    int written;
    va_list args;
    va_start( args, format );

    written = vsnprintf( str, size, format, args );
    if( written<0 || written>=size )
        return 0;
    else
        return written;

    va_end( args );
}

inline void* calloc_or_die( size_t num, size_t size ) {
    void* ret;
    ret = calloc( num, size );
    true_or_die( ret!=NULL, "Error: calloc failed" );
    return ret;
}

inline void* malloc_or_die( size_t size ) {
    void* ret;
    ret = malloc( size );
    true_or_die( ret!=NULL, "Error: malloc failed" );
    return ret;
}

inline void* realloc_or_die( void* ptr, size_t size ) {
    void* ret;
    ret = realloc( ptr, size );
    true_or_die( ret!=NULL, "Error: realloc failed" );
    return ret;
}

bool str_matches( const char* given, int num_args, ... ) {
    bool ret = FALSE;
    const char* str;
    va_list args;
    va_start( args, num_args );

    while( num_args-- > 0 ) {
        str = va_arg(args, const char*);
        if( strcmp(str,given) == 0 ) {
            ret = TRUE;
            break; /* found a match */
        }
    }
    va_end( args );

    return ret;
}

static double startTime;
static const char* printName;
static int verbose;
void print_init(const char* name) {
    struct timeval now;

    /* initialize the start time */
    gettimeofday(&now, NULL);
    startTime = now.tv_sec + now.tv_usec / 1000000.0;

    printName = name;
}

void print_set_verbosity(int verbosity) {
    verbose = verbosity;
}

void print_timestamp() {
    struct timeval now;
    double t;

    gettimeofday(&now,NULL);
    t = now.tv_sec + now.tv_usec / 1000000.0 - startTime;

    fprintf( stdout, "%.3f: ", t );
}

void print( const char* format, ... ) {
    va_list args;
    va_start( args, format );

    print_timestamp();
    fprintf( stdout, "[%s] ", printName );
    vfprintf( stdout, format, args );
    fprintf( stdout, "\n" );

    va_end( args );
}

void print_verbose( int level, const char* format, ... ) {
    va_list args;

    if( verbose < level )
        return;

    va_start( args, format );

    print_timestamp();
    fprintf( stdout, "[%s] ", printName );
    vfprintf( stdout, format, args );
    fprintf( stdout, "\n" );

    va_end( args );
}

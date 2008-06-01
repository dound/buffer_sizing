/* Filename: common.c */

#include <arpa/inet.h>
#include <stdarg.h>
#include <stdio.h>      /* snprintf() */
#include <stdlib.h>     /* calloc, malloc, realloc */
#include <string.h>
#include <sys/time.h>   /* struct timeval */
#include "common.h"
#include "debug.h"

uint32_t get_time_passed_ms( struct timeval* start, struct timeval* end ) {
    uint32_t start_ms;
    uint32_t end_ms;

    start_ms = start->tv_sec*1000 + start->tv_usec / 1000;
    end_ms   =   end->tv_sec*1000 +   end->tv_usec / 1000;
    return( end_ms - start_ms );
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

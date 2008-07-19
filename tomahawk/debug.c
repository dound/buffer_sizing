/* Filename: debug.c */

#include <arpa/inet.h>
#include <netinet/in.h>
#include <pthread.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "debug.h"

/** file-scope lock to provide mutual exclusion to print statements */
static pthread_mutex_t print_mutex = PTHREAD_MUTEX_INITIALIZER;

#ifdef _DEBUG_
/** returns the user-defined thread ID */
unsigned debug_tid();

/** returns the user-defined thread name */
char* debug_tname();

/** prints a message about what thread this is */
void debug_print_tid() {
    fprintf( stderr, "Thread %u: ", debug_tid() );
}

/** prints a message about what thread this is */
void debug_print_tname() {
    fprintf( stderr, "%15s: ", debug_tname() );
}
#endif

void debug_print( const char* format, ... ) {
#ifdef _DEBUG_
    va_list args;
    va_start( args, format );

    pthread_mutex_lock( &print_mutex );
    debug_print_tname();
    vfprintf( stderr, format, args );
    pthread_mutex_unlock( &print_mutex );

    va_end( args );
#endif
}

void debug_print_more( const char* format, ... ) {
#ifdef _DEBUG_
    va_list args;
    va_start( args, format );

    pthread_mutex_lock( &print_mutex );
    vfprintf( stderr, format, args );
    pthread_mutex_unlock( &print_mutex );

    va_end( args );
#endif
}

/** Prints the specified IP address. */
void debug_print_ip( addr_ip_t ip ) {
#ifdef _DEBUG_
    char str_ip[STRLEN_IP];

    ip_to_string( str_ip, ip );

    pthread_mutex_lock( &print_mutex );
    fprintf( stderr, "%s", str_ip );
    pthread_mutex_unlock( &print_mutex );
#endif
}

void debug_println( const char* format, ... ) {
#ifdef _DEBUG_
    va_list args;
    va_start( args, format );

    pthread_mutex_lock( &print_mutex );
    debug_print_tname();
    vfprintf( stderr, format, args );
    fprintf( stderr, "\n" );
    pthread_mutex_unlock( &print_mutex );

    va_end( args );
#endif
}

#ifdef _DEBUG_
static pthread_key_t key_id;
static pthread_key_t key_name;
static int id_counter;
static pthread_mutex_t key_lock;
#endif

void debug_pthread_init_init() {
#ifdef _DEBUG_
    id_counter = 0;
    pthread_key_create( &key_id, free );
    pthread_key_create( &key_name, free );
    pthread_mutex_init( &key_lock, NULL );
#endif
}

void debug_pthread_init( const char* shortName, const char* longName ) {
#ifdef _DEBUG_
    unsigned* id;
    char* name;

    id = malloc( sizeof(unsigned) );
    assert( id );

    name = malloc( (strlen(shortName)+1) * sizeof(char) );
    strcpy( name, shortName );
    assert( name );

    pthread_mutex_lock( &key_lock );
    *id = id_counter++;
    pthread_mutex_unlock( &key_lock );

    pthread_setspecific( key_id, id );
    pthread_setspecific( key_name, name );

    debug_println( "%s is now alive", longName );
#endif
}

#ifdef _DEBUG_
unsigned debug_tid() {
    return *(unsigned*)pthread_getspecific( key_id );
}
char* debug_tname() {
    return (char*)pthread_getspecific( key_name );
}
#endif

#ifndef _DEBUG_
void debug_die( const char* format, ... ) { }
#endif

void die( const char* format, ... ) {
    va_list args;
    va_start( args, format );

    pthread_mutex_lock( &print_mutex );
#ifdef _DEBUG_
    debug_print_tname();
#endif
    vfprintf( stderr, format, args );
    fprintf( stderr, "\n" );
    pthread_mutex_unlock( &print_mutex );

    va_end( args );
    assert( 0 );
    exit( 1 );
}

#ifndef _DEBUG_
void debug_true_or_die( int bool, const char* format, ... ) { }
#endif

void true_or_die( int bool, const char* format, ... ) {
    if( !bool ) {
        va_list args;
        va_start( args, format );

        pthread_mutex_lock( &print_mutex );
        vfprintf( stderr, format, args );
        fprintf( stderr, "\n" );
        pthread_mutex_unlock( &print_mutex );

        va_end( args );
        assert( 0 );
        exit( 1 );
    }
}

const char* quick_ip_to_string( addr_ip_t ip ) {
    static char str_ip[STRLEN_IP];
    ip_to_string( str_ip, ip );
    return str_ip;
}

/* Filename: debug.h
 * Purpose:  define some helper functions for debugging
 */

#ifndef DEBUG_H
#define DEBUG_H

#include <assert.h>
#include <stdarg.h>
#include "common.h"

/**
 * If _DEBUG_ is defined, this function converts, formats, and prints its
 * arguments to stderr as if fprintf( stderr, format, ... ) was called.  The
 * print is prefixed with "Thread <tid>:".
 */
void debug_print( const char* format, ... );

/**
 * If _DEBUG_ is defined, this function converts, formats, and prints its
 * arguments to stderr as if fprintf( stderr, format, ... ) was called.
 */
void debug_print_more( const char* format, ... );

/**
 * If _DEBUG_ is defined, this function converts, formats, and prints its
 * arguments to stderr as if fprintf( stderr, format, ... ) was called and then
 * prints a newline character.  The print is prefixed with "Thread <tid>:".
 */
void debug_println( const char* format, ... );

/** Prints the specified IP address. */
void debug_print_ip( addr_ip_t ip );

/** initializes the debugging code for assigning manual thread IDs */
void debug_pthread_init_init();

/** initializes the debug thread ID for the calling thread and names it */
void debug_pthread_init( const char* shortName, const char* longName );

/**
 * Terminates the process if the argument is not true if _DEBUG_ is defined.
 *
 * If message is not NULL, then an error message will be sent to stderr. The
 * The formatted message will be printed to stderr as if fprintf( stderr,
 * format, .. ) was called except a newline character will be appended.
 */
void debug_true_or_die( int bool, const char* format, ... );

/**
 * Terminates the process if _DEBUG_ is defined.
 *
 * If message is not NULL, then an error message will be sent to stderr. The
 * The formatted message will be printed to stderr as if fprintf( stderr,
 * format, .. ) was called except a newline character will be appended.
 */
void debug_die( const char* format, ... );

/**
 * Terminates the process.
 *
 * If message is not NULL, then an error message will be sent to stderr. The
 * The formatted message will be printed to stderr as if fprintf( stderr,
 * format, .. ) was called except a newline character will be appended.
 */
void die( const char* format, ... );

/**
 * This is a non-reentrant ip to string conversion.  The returned buffer is
 * reused each call.
 */
const char* quick_ip_to_string( addr_ip_t ip );

/**
 * Terminates the process if the argument is not true.
 *
 * If message is not NULL, then an error message will be sent to stderr. The
 * The formatted message will be printed to stderr as if fprintf( stderr,
 * format, .. ) was called except a newline character will be appended.
 */
void true_or_die( int bool, const char* format, ... );

#ifdef _DEBUG_
# define debug_true_or_die true_or_die
# define debug_die die
#endif

#endif /* DEBUG_H */

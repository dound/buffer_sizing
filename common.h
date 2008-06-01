/**
 * Filename: common.h
 * Purpose: define the basic structures
 */

#ifndef COMMON_H
#define COMMON_H

#include <netinet/in.h>
#ifdef _LINUX_
#include <stdint.h> /* uintX_t */
#else
#include <sys/types.h>
#endif
#include <sys/param.h> /* __BYTE_ORDER, __BIG_ENDIAN, __LITTLE_ENDIAN */
#include <stdio.h>     /* FILE */

/** length in bytes of an IPv4 address */
#define IP_ADDR_LEN 4

/* 4 octets of 3 chars each, 3 periods, 1 nul => 16 chars */
#define STRLEN_IP  16

/** byte is an 8-bit unsigned integer */
typedef uint8_t byte;

/** boolean is stored in an 8-bit unsigned integer */
typedef uint8_t bool;
#define TRUE 1
#define FALSE 0

/** IP address type */
typedef uint32_t addr_ip_t;

/**
 * Returns the numbers of miliiseconds passed between the two times.  Cannot be
 * greater than about 486 days or the return value will be incorrect.
 */
uint32_t get_time_passed_ms( struct timeval* start, struct timeval* end );

/**
 * Converts ip into a string and stores it in buf.  buf must be at least
 * STRLEN_IP bytes long.
 */
void ip_to_string( char* buf, addr_ip_t ip );

/**
 * Wrapper for snprintf.
 *
 * @return 0 on failure and otherwise returns the number of bytes written, not
 *         including the terminating NUL character.
 */
unsigned my_snprintf( char* str, size_t size, const char* format, ... );

inline void* calloc_or_die( size_t num, size_t size );
inline void* realloc_or_die( void* ptr, size_t size );
inline void* malloc_or_die( size_t size );

/** Returns true if any of the var_args match given with strcmp. */
bool str_matches( const char* given, int num_args, ... );

#endif /* COMMON_H */

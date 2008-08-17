/**
 * Filename: nf2helpers.h
 * Purpose: accessor and mutators for interesting nf2 registers
 */

#ifdef _LINUX_
#include <stdint.h> /* uintX_t */
#endif
#include "nf2util.h"

extern nf2_device_t nf2;

uint32_t get_rate_limit( int queue );
void set_rate_limit( int queue, uint32_t shift );
uint32_t get_bytes_sent();
uint32_t get_buffer_size_packets( int queue );
void set_buffer_size_packets( int queue, uint32_t size );
uint32_t get_queue_occupancy_packets( int queue );

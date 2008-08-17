#include <stdio.h>
#include <stdlib.h>
#include "nf2helpers.h"
#include "reg_defines_bsr.h"

nf2_device_t nf2;

uint32_t get_rate_limit( int queue ) {
    uint32_t enabled, val;

    if( queue != 1 ) {
        fprintf(stderr, "Error: only queue 1 (not queue %d) can do rate limiting\n", queue);
        exit( 1 );
    }

    readReg( &nf2, RATE_LIMIT_ENABLE_REG, &enabled );
    readReg( &nf2, RATE_LIMIT_SHIFT_REG, &val );
    return enabled ? val : 0;
}

void set_rate_limit( int queue, uint32_t shift ) {
    if( queue != 1 ) {
        fprintf(stderr, "Error: only queue 1 (not queue %d) can do rate limiting\n", queue);
        exit( 1 );
    }

    uint32_t enabled = shift ? 1 : 0;
    writeReg( &nf2, RATE_LIMIT_ENABLE_REG, enabled );
    writeReg( &nf2, RATE_LIMIT_SHIFT_REG, shift );
}

uint32_t get_bytes_sent( int queue ) {
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

inline unsigned get_buffer_size_packets_reg( int queue ) {
    unsigned reg;
    switch( queue ) {
    case 0: reg = OQ_MAX_PKTS_IN_Q_REG_0; break;
    case 1: reg = OQ_MAX_PKTS_IN_Q_REG_2; break;
    case 2: reg = OQ_MAX_PKTS_IN_Q_REG_4; break;
    case 3: reg = OQ_MAX_PKTS_IN_Q_REG_6; break;
    }
    return reg;
}

uint32_t get_buffer_size_packets( int queue ) {
    uint32_t val;
    readReg( &nf2, get_buffer_size_packets_reg(queue), &val );
    return val;
}

void set_buffer_size_packets( int queue, uint32_t size ) {
    writeReg( &nf2, get_buffer_size_packets_reg(queue), size );
}

uint32_t get_queue_occupancy_packets( int queue ) {
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

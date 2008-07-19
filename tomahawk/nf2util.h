/* ****************************************************************************
 * $Id: nf2util.h 3546 2008-04-03 00:12:27Z grg $
 *
 * Module: nf2util.h
 * Project: NetFPGA 2 Linux Kernel Driver
 * Description: Header file for kernel driver
 *
 * Change history:
 *   Modified by David Underhill:
 *     - Tidied up how nf2 device info is gathered and stored
 *     - Added method for pretty-printing device info
 */

#ifndef _NF2UTIL_H
#define _NF2UTIL_H	1

#define PATHLEN         80
#define DEVICE_STR_LEN 120
#define DEFAULT_IFACE "nf2c0"

/** Encapsulates version info about a NetFPGA. */
typedef struct {
    unsigned nf2_device_id;
    unsigned nf2_revision;
    char nf2_device_str[DEVICE_STR_LEN];
} nf2_device_info_t;

/*
 * Structure to represent an nf2 device to a user mode programs
 */
typedef struct nf2device {
    char *device_name;
    int fd;
    int net_iface;
    nf2_device_info_t info;
} nf2_device_t;

/* Function declarations */
int readReg(nf2_device_t *nf2, unsigned reg, unsigned *val);
int writeReg(nf2_device_t *nf2, unsigned reg, unsigned val);
int check_iface(nf2_device_t *nf2);
int openDescriptor(nf2_device_t *nf2);
int closeDescriptor(nf2_device_t *nf2);

/** Connects to the hw and popuates nf2.  On failure, the program terminates */
void hw_init( nf2_device_t* nf2 );

#define STR_HW_INFO_MAX_LEN (2*DEVICE_STR_LEN + 4*10) /* 4 ints, 2 strings */

/**
 * Fills buf with a string representation of the basic hardware information.  It
 * consumes no more than STR_HW_INFO_MAX_LEN.
 *
 * @param buf  buffer to place the string in
 * @param len  length of the buffer
 *
 * @return number of bytes written to create the table, or 0 if there was no
 *         enough space in buf to create the table
 */
int hw_info_to_string( nf2_device_t* nf2, char* buf, int len );

#endif

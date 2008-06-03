/**
 * Filename: que_occ.c
 * Purpose: constantly logs the value to the regiser
 */

#ifdef _LINUX_
#include <stdint.h> /* uintX_t */
#endif
#include <fcntl.h>
#include <signal.h>
#include <string.h>
#include <sys/stat.h>
#include <sys/time.h>
#include <unistd.h>
#include "io_wrapper.h"
#include "nf2util.h"
#include "reg_defines_bsr.h"

static int alive = 1;

static void sig_int_handler( int sig ) {
    alive = 0;
}

typedef struct {
  struct timeval t;
  uint32_t val;
} data_t;

void write_log( char* fn ) {
    data_t d;

    /* connect to the hardware */
    nf2_device_t nf2;
    hw_init( &nf2 );
    signal( SIGINT, sig_int_handler );

    /* open a log file */
    mode_t perms_owner_rw = S_IRUSR | S_IWUSR; /* read-write permission for owner */
    int fd = open( fn, O_RDWR | O_CREAT, perms_owner_rw );
    if( fd < 0 ) {
        perror("open write log failed");
        exit( 1 );
    }

    uint32_t shift = 8;
    uint32_t enabled = shift ? 1 : 0;
    writeReg( &nf2, RATE_LIMIT_ENABLE_REG, enabled );
    writeReg( &nf2, RATE_LIMIT_SHIFT_REG, shift );

    /* read the reg a lot */
    while( alive ) {
        gettimeofday( &d.t, NULL );
        readReg( &nf2, OQ_NUM_WORDS_IN_Q_REG_2, &d.val );
	writen(fd,&d,sizeof(d));
	usleep( 1000 );
    }

    close( fd );
}

void read_log( char* fn ) {
    data_t d;

    /* open a log file */
    int fd = open( fn, O_RDONLY );
    if( fd < 0 ) {
        perror( "open read log failed" );
        exit( 1 );
    }

    /* read the reg a lot */
    int ret;
    while( (ret=readn(fd,&d,sizeof(d))) )
        printf( "%f %u\n", d.t.tv_sec + d.t.tv_usec / 1000000.0, d.val );

    close( fd );
}

int main( int argc, char** argv ) {
    if( argc != 3 )
        exit( 1 );

    if( strcmp(argv[1],"-l")==0 )
        write_log( argv[2] );
    else if( strcmp(argv[1],"-r")==0 )
        read_log( argv[2] );
    else
      printf("bad\n");

    return 0;
}

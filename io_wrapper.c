/* Filename: io_wrapper.c */

#include <errno.h>            /* errno */
#include <unistd.h>
#include "common.h"
#include "io_wrapper.h"

ssize_t readn( int fd, void* buf, unsigned n ) {
    size_t nleft;
    ssize_t nread;
    char* chbuf;

    /* loop until n bytes have been read from fd, or an EOF or error occurs */
    chbuf = (char*)buf;
    nleft = n;
    while( nleft > 0 ) {
        if( (nread = read(fd, chbuf, nleft)) < 0 ) {
            if( errno == EINTR )
                continue;  /* interrupt: no bytes read: try again */
            else
                return -1; /* problem with read: error! */
        }
        else if( nread == 0 )
            break;         /* EOF */

        nleft -= nread;
        chbuf += nread;
    }

    return (n - nleft);
}

int writen( int fd, const void* buf, unsigned n ) {
    size_t nleft;
    ssize_t nwritten;
    char* chbuf;

    /* loop until n bytes have been written into fd, or an error occurs */
    chbuf = (char*)buf;
    nleft = n;
    while( nleft > 0 ) {
        if( (nwritten = write(fd, chbuf, nleft)) <= 0 ) {
            if( nwritten < 0 && errno == EINTR )
                continue;  /* interrupt: no bytes written: try again */
            else
                return -1; /* problem with write: error! */
        }

        nleft -= nwritten;
        chbuf += nwritten;
    }

    return 0; /* indicates success */
}

/*
 * Filename: io_wrapper.h
 *
 * Purpose:  Define wrapper functions to handle some error conditions of various
 *           UNIX I/O methods.  This also includes various socket-related header
 *           files.
 *
 * Acknowledgements: The general wrapper function design as well as some of
 *           their implementations (noted below) are based on the
 *           following book:
 *            W. Richard Stevens, Bill Fenner, and Andrew M. Rudoff. UNIX Network
 *            Programming. Addison-Wesley. Volume 1, Edition 3, 2007. 89.
 */

#ifndef IO_WRAPPER_H
#define IO_WRAPPER_H

/**
 * Read n bytes into buf from the file descriptor fd.
 *
 * The implementation of this function is based on code in the following book:
 *  W. Richard Stevens, Bill Fenner, and Andrew M. Rudoff. UNIX Network
 *  Programming. Addison-Wesley. Volume 1, Edition 3, 2007. 89.
 *
 * @param fd   file descriptor to read from
 * @param buf  buffer to read into; must be at least size n
 * @param n    number of bytes to read into buf
 *
 * @return Number of bytes read into buf, or -1 if an error occurs.  If an error
 *         does not occur, the number of bytes read will be n unless an EOF was
 *         encountered.
 *
 */
ssize_t readn( int fd, void* buf, unsigned n );

/**
 * Write n bytes from buf into the file descriptor fd.
 *
 * The implementation of this function is based on code in the following book:
 *  W. Richard Stevens, Bill Fenner, and Andrew M. Rudoff. UNIX Network
 *  Programming. Addison-Wesley. Volume 1, Edition 3, 2007. 89.
 *
 * @param fd   file descriptor to write to
 * @param buf  buffer to read from; must be at least size n
 * @param n    number of bytes to write from buf into fd
 *
 * @return 0 on success or -1 if an error occurs.
 */
int writen( int fd, const void* buf, unsigned n );

#endif /* IO_WRAPPER_H */

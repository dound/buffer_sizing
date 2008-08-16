/* ****************************************************************************
 * $Id: nf2util.c 3546 2008-04-03 00:12:27Z grg $
 *
 * Module: nf2util.c
 * Project: NetFPGA 2 Linux Kernel Driver
 * Description: Utility functions for user mode programs
 *
 * Change history:
 *
 */

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>

#include <sys/types.h>
#include <sys/stat.h>
#include <sys/socket.h>
#include <sys/ioctl.h>

#include <net/if.h>
#include <arpa/inet.h>

#include "../debug.h"
#include "nf2.h"
#include "nf2util.h"
#include "reg_defines_bsr.h" /* buffer sizing router reg defines */

/* Function declarations */
static int readRegNet(nf2_device_t *nf2, unsigned reg, unsigned *val);
static int readRegFile(nf2_device_t *nf2, unsigned reg, unsigned *val);
static int writeRegNet(nf2_device_t *nf2, unsigned reg, unsigned val);
static int writeRegFile(nf2_device_t *nf2, unsigned reg, unsigned val);
static void read_info(nf2_device_t *nf2);

/*
 * readReg - read a register
 */
int readReg(nf2_device_t *nf2, unsigned reg, unsigned *val)
{
	if (nf2->net_iface)
	{
		return readRegNet(nf2, reg, val);
	}
	else
	{
		return readRegFile(nf2, reg, val);
	}
}

/*
 * readRegNet - read a register, using a network socket
 */
static int readRegNet(nf2_device_t *nf2, unsigned reg, unsigned *val)
{
        struct ifreq ifreq;
	struct nf2reg nf2reg;
	int ret;

	nf2reg.reg = reg;

	/* Set up the ifreq structure */
	ifreq.ifr_data = (char *)&nf2reg;
        strncpy(ifreq.ifr_ifrn.ifrn_name, nf2->device_name, IFNAMSIZ);
        /*if (setsockopt(nf2->fd, SOL_SOCKET, SO_BINDTODEVICE,
                       (char *)&ifreq, sizeof(ifreq)) < 0) {
                perror("sendpacket: setting SO_BINDTODEVICE");
                return -1;
        } */

	/* Call the ioctl */
	if ((ret = ioctl(nf2->fd, SIOCREGREAD, &ifreq)) == 0)
	{
		*val = nf2reg.val;
		return 0;
	}
	else
	{
                perror("sendpacket: ioctl failed in readRegNet");
                return -1;
	}
}

/*
 * readRegFile - read a register, using a file descriptor
 */
static int readRegFile(nf2_device_t *nf2, unsigned reg, unsigned *val)
{
	struct nf2reg nf2reg;
	int ret;

	nf2reg.reg = reg;

	/* Call the ioctl */
	if ((ret = ioctl(nf2->fd, SIOCREGREAD, &nf2reg)) == 0)
	{
		*val = nf2reg.val;
		return 0;
	}
	else
	{
                perror("sendpacket: ioctl failed in readRegFile");
                return -1;
	}
}


/*
 * writeReg - write a register
 */
int writeReg(nf2_device_t *nf2, unsigned reg, unsigned val)
{
	if (nf2->net_iface)
	{
		return writeRegNet(nf2, reg, val);
	}
	else
	{
		return writeRegFile(nf2, reg, val);
	}
}


/*
 * writeRegNet - write a register, using a network socket
 */
static int writeRegNet(nf2_device_t *nf2, unsigned reg, unsigned val)
{
        struct ifreq ifreq;
	struct nf2reg nf2reg;
	int ret;

	nf2reg.reg = reg;
	nf2reg.val = val;

	/* Set up the ifreq structure */
	ifreq.ifr_data = (char *)&nf2reg;
        strncpy(ifreq.ifr_ifrn.ifrn_name, nf2->device_name, IFNAMSIZ);
        /*if (setsockopt(nf2->fd, SOL_SOCKET, SO_BINDTODEVICE,
                       (char *)&ifreq, sizeof(ifreq)) < 0) {
                perror("sendpacket: setting SO_BINDTODEVICE");
                return -1;
        } */

	/* Call the ioctl */
	if ((ret = ioctl(nf2->fd, SIOCREGWRITE, &ifreq)) == 0)
	{
		return 0;
	}
	else
	{
                perror("sendpacket: ioctl failed in writeRegNet");
                return -1;
	}
}


/*
 * writeRegFile - write a register, using a file descriptor
 */
static int writeRegFile(nf2_device_t *nf2, unsigned reg, unsigned val)
{
	struct nf2reg nf2reg;
	int ret;

	nf2reg.reg = reg;
	nf2reg.val = val;

	/* Call the ioctl */
	if ((ret = ioctl(nf2->fd, SIOCREGWRITE, &nf2reg)) == 0)
	{
		return 0;
	}
	else
	{
                perror("sendpacket: ioctl failed in writeRegFile");
                return -1;
	}
}

/*
 * Check the iface name to make sure we can find the interface
 */
int check_iface(nf2_device_t *nf2)
{
	struct stat buf;
	char filename[PATHLEN];

	/* See if we can find the interface name as a network device */

	/* Test the length first of all */
	if (strlen(nf2->device_name) > IFNAMSIZ)
	{
		fprintf(stderr, "Interface name is too long: %s\n", nf2->device_name);
		return -1;
	}

	/* Check for /sys/class/net/iface_name */
	strcpy(filename, "/sys/class/net/");
	strcat(filename, nf2->device_name);
	if (stat(filename, &buf) == 0)
	{
		fprintf(stderr, "Found net device: %s\n", nf2->device_name);
		nf2->net_iface = 1;
		return 0;
	}

	/* Check for /dev/iface_name */
	strcpy(filename, "/dev/");
	strcat(filename, nf2->device_name);
	if (stat(filename, &buf) == 0)
	{
		fprintf(stderr, "Found dev device: %s\n", nf2->device_name);
		nf2->net_iface = 0;
		return 0;
	}

	fprintf(stderr, "Can't find device: %s\n", nf2->device_name);
	return -1;
}

/*
 * Open the descriptor associated with the device name
 */
int openDescriptor(nf2_device_t *nf2)
{
        struct ifreq ifreq;
	char filename[PATHLEN];
	struct sockaddr_in address;
	int i;
	struct sockaddr_in *sin = (struct sockaddr_in *) &ifreq.ifr_addr;
	int found = 0;

	if (nf2->net_iface)
	{
		/* Open a network socket */
		nf2->fd = socket(AF_INET, SOCK_DGRAM, 0);
		if (nf2->fd == -1)
		{
                	perror("socket: creating socket");
                	return -1;
		}
		else
		{
			/* Root can bind to a network interface.
			   Non-root has to bind to a network address. */
			if (geteuid() == 0)
			{
				strncpy(ifreq.ifr_ifrn.ifrn_name, nf2->device_name, IFNAMSIZ);
				if (setsockopt(nf2->fd, SOL_SOCKET, SO_BINDTODEVICE,
					(char *)&ifreq, sizeof(ifreq)) < 0) {
					perror("setsockopt: setting SO_BINDTODEVICE");
					return -1;
				}

			}
			else
			{
				/* Attempt to find the IP address for the interface */
				for (i = 1; ; i++)
				{
					/* Find interface number i*/
					ifreq.ifr_ifindex = i;
					if (ioctl (nf2->fd, SIOCGIFNAME, &ifreq) < 0)
						break;

					/* Check if we've found the correct interface */
					if (strcmp(ifreq.ifr_name, nf2->device_name) != 0)
						continue;

					/* If we get to here we've found the IP */
					found = 1;
					break;
				}

				/* Verify that we found the interface */
				if (!found)
				{
					fprintf(stderr, "Can't find device: %s\n", nf2->device_name);
					return -1;
				}

				/* Attempt to get the IP address associated with the interface */
				if (ioctl (nf2->fd, SIOCGIFADDR, &ifreq) < 0)
				{
					perror("ioctl: calling SIOCGIFADDR");

					fprintf(stderr, "Unable to find IP address for device: %s\n", nf2->device_name);
					fprintf(stderr, "Either run this program as root or ask an administrator\n");
					fprintf(stderr, "to assign an IP address to the device\n");
					return -1;
				}

				/* Set the addres and attempt to bind to the socket */
				address.sin_family = AF_INET;
				address.sin_addr.s_addr = sin->sin_addr.s_addr;
				address.sin_port = htons(0);
				if (bind(nf2->fd,(struct sockaddr *)&address,sizeof(address)) == -1) {
					perror("bind: binding");
					return -1;
				}
			}
		}
	}
	else
	{
		strcpy(filename, "/dev/");
		strcat(filename, nf2->device_name);
		FILE* file = fopen(filename, "w+");
		if( !file ) {
		  perror( "fopen failed" );
		  return -1;
		}

		nf2->fd = fileno( file );
		if (nf2->fd == -1)
		{
                	perror("fileno: creating descriptor");
                	return -1;
		}
	}

          read_info( nf2 );
	return 0;
}

/*
 * Close the descriptor associated with the device name
 */
int closeDescriptor(nf2_device_t *nf2)
{
	if (nf2->net_iface)
	{
		close(nf2->fd);
	}
	else
	{
		close(nf2->fd);
	}

	return 0;
}


/*
 *  Read the version info from the board
 */
void read_info( nf2_device_t *nf2  )
{
  int i;

  // Read the version and revision
  readReg(nf2, DEVICE_ID_REG, &nf2->info.nf2_device_id);
  readReg(nf2, DEVICE_REVISION_REG, &nf2->info.nf2_revision);

  // Read the version string
  for (i = 0; i < (DEVICE_STR_LEN / 4) - 2; i++)
  {
    readReg(nf2, DEVICE_STR_REG + i * 4, (unsigned *)(nf2->info.nf2_device_str + i * 4));

    // Perform byte swapping if necessary
    *(unsigned *)(nf2->info.nf2_device_str + i * 4) = ntohl(*(unsigned *)(nf2->info.nf2_device_str + i * 4));
  }
  nf2->info.nf2_device_str[DEVICE_STR_LEN - 1] = '\0';
}

void hw_init( nf2_device_t* nf2 ) {
    /* connect to the hardware */
    nf2->device_name = DEFAULT_IFACE;
    check_iface( nf2 );
    if( openDescriptor( nf2 ) != 0 )
        die( "Error: failed to connect to the hardware" );
    else {
        char about[STR_HW_INFO_MAX_LEN];
        hw_info_to_string( nf2, about, STR_HW_INFO_MAX_LEN );
        debug_println( "Now connected to the NetFPGA ... %s", about );
    }
}

int hw_info_to_string( nf2_device_t* nf2, char* buf, int len ) {
    return my_snprintf( buf, len, "\
Basic NetFPGA Hardware Info:\n\
          Device Name: %s\n\
            Device ID: %u\n\
         NF2 Revision: %u\n\
    NF2 Device String: %s\n",
                        nf2->device_name,
                        nf2->info.nf2_device_id,
                        nf2->info.nf2_revision,
                        nf2->info.nf2_device_str );
}

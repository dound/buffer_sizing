# Makefile for building the encapsulation Linux Kernel Module

KDIR := /lib/modules/$(shell uname -r)/build
PWD  := $(shell pwd)

obj-m := lkm_encap.o
module = $(patsubst %.o,%.ko,$(obj-m))

.PHONY: clean cycle install im module mod uninstall um

# build the kernel module
module mod:
	$(MAKE) -C $(KDIR) SUBDIRS=$(PWD) modules

# install the kernel module
install im:
	sudo /sbin/insmod $(module)

# uninstall the kernel module
uninstall um:
	sudo /sbin/rmmod $(module)

# uninstall, build, and install
cycle: um mod im

# clean up byproducts
clean:
	rm -rf Modules.symvers .tmp_versions .*.cmd *.mod.c *.ko *.o

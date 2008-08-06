# Makefile for the Tomahawk TCP flow generator
# ------------------------------------------------------------------------------
# make        -- builds Tomahawk and all dependencies in the default mode
# make debug  -- builds Tomahawk in debug mode
# make release-- builds Tomahawk in release mode
# make clean  -- clean up byproducts

# utility programs used by this Makefile
CC   = gcc
MAKE = gmake --no-print-directory

# set system-dependent variables
OSTYPE = $(shell uname)
ifeq ($(OSTYPE),Linux)
ARCH=-D_LINUX_
ENDIAN=-D_LITTLE_ENDIAN_
LIB_SOCKETS =
endif
ifeq ($(OSTYPE),SunOS)
ARCH=-D_SOLARIS_
ENDIAN=-D_BIG_ENDIAN_
LIB_SOCKETS = -lnsl -lsocket
endif

# define names of our build targets
APP = tomahawk
DAEMON = buf_size_daemon
LOGGER = logger
TC = tgen_controller

# compiler and its directives
DIR_INC       = -I.
DIR_LIB       =
LIBS          = $(LIB_SOCKETS) -lpthread
FLAGS_CC_BASE = -c -Wall $(ARCH) $(ENDIAN) $(DIR_INC)

# compiler directives for debug and release modes
BUILD_TYPE = debug
ifeq ($(BUILD_TYPE),debug)
FLAGS_CC_BUILD_TYPE = -g -D_DEBUG_
else
FLAGS_CC_BUILD_TYPE = -O3
endif

# put all the flags together
CFLAGS = $(FLAGS_CC_BASE) $(FLAGS_CC_BUILD_TYPE)

# project sources
SRCS = common.c debug.c io_wrapper.c tomahawk.c
OBJS = $(patsubst %.c,%.o,$(SRCS))
DEPS = $(patsubst %.c,.%.d,$(SRCS))

# daemon sources
D_SRCS = buf_size_daemon.c nf2util.c
D_OBJS = $(patsubst %.c,%.o,$(D_SRCS)) common.o debug.o io_wrapper.o
D_DEPS = $(patsubst %.c,.%.d,$(D_SRCS))

# logger sources
L_SRCS = logger.c
L_OBJS = $(patsubst %.c,%.o,$(L_SRCS)) common.o debug.o io_wrapper.o nf2util.c
L_DEPS = $(patsubst %.c,.%.d,$(L_SRCS))

# daemon sources
T_SRCS = tgen_controller.c
T_OBJS = $(patsubst %.c,%.o,$(T_SRCS)) common.o debug.o io_wrapper.o
T_DEPS = $(patsubst %.c,.%.d,$(T_SRCS))

# include the dependencies once we've built them
ifdef INCLUDE_DEPS
include $(DEPS) $(D_DEPS) $(L_DEPS) $(T_DEPS)
endif

#########################
## PHONY TARGETS
#########################
# note targets which don't produce a file with the target's name
.PHONY: all clean clean-all clean-deps daemon debug release deps $(APP).ir tc

# build the program
all: $(APP) $(DAEMON) $(LOGGER) $(TC)

# clean up by-products (except dependency files)
clean:
	rm -f *.o $(APP) $(DAEMON) $(LOGGER)

# clean up all by-products
clean-all: clean clean-deps

# clean up dependency files
clean-deps:
	rm -f .*.d

# shorthand for building debug or release builds
debug release:
	@$(MAKE) BUILD_TYPE=$@ all

# build the dependency files
deps: $(DEPS)

# includes are ready build command
IR=ir
$(APP).$(IR): $(OBJS)
	$(CC) -o $(APP) $(OBJS) $(DIR_LIB) $(LIBS)

$(DAEMON).$(IR): $(D_OBJS)
	$(CC) -o $(DAEMON) $(D_OBJS) $(DIR_LIB) $(LIBS)

$(LOGGER).$(IR): $(L_OBJS)
	$(CC) -o $(LOGGER) $(L_OBJS) $(DIR_LIB) $(LIBS)

$(TC).$(IR): $(T_OBJS)
	$(CC) -o $(TC) $(T_OBJS) $(DIR_LIB) $(LIBS)

#########################
## REAL TARGETS
#########################
$(APP): deps
	@$(MAKE) BUILD_TYPE=$(BUILD_TYPE) INCLUDE_DEPS=1 $@.$(IR)

daemon : $(DAEMON)
$(DAEMON): deps
	@$(MAKE) BUILD_TYPE=$(BUILD_TYPE) INCLUDE_DEPS=1 $@.$(IR)

$(LOGGER): deps
	@$(MAKE) BUILD_TYPE=$(BUILD_TYPE) INCLUDE_DEPS=1 $@.$(IR)

tc : $(TC)
$(TC): deps
	@$(MAKE) BUILD_TYPE=$(BUILD_TYPE) INCLUDE_DEPS=1 $@.$(IR)

$(DEPS) $(D_DEPS) $(L_DEPS) $(T_DEPS): .%.d: %.c
	$(CC) -MM $(CFLAGS) $(DIRS_INC) $< > $@

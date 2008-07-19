#!/bin/bash
set -o nounset

# some constants
DEMO_ROOT=/root/demo
BITFILE=latest-router_buffer_sizing.bit
SW_DIR=decap_sw
SW_NAME=sr
DAEMON_NAME=buf_size_daemon
EC_SETUP_SCRIPT=enable_ec.sh
EC_CLEANUP_SCRIPT=disable_ec.sh
GUI_EC_PORT=10272
TGEN_IP=171.64.74.14 #nf-test5

# choose whether to use LA1/HOU1 or LA2/HOU2
use_net1=1
if [ $use_net1 -eq 1 ]; then
    LA_IP="64.57.23.66"
    HOU_IP="64.57.23.74"
    LA_NF2C1_IP="64.57.23.38"
    LA_ROUTES="/sbin/route add -net 64.57.23.32 mss 1400 gw $LA_NF2C1_IP netmask 255.255.255.248 nf2c1"
    echo "will setup LA1/HOU1"
else
    LA_IP="64.57.23.67"
    HOU_IP="64.57.23.75"
    LA_NF2C1_IP="64.57.23.xx"
    LA_ROUTES="echo 'i dont know yet'"
    echo "will setup LA2/HOU2"

    echo "error: what is LA2's NF2C1 IP?  not yet set ..."
    exit 1
fi

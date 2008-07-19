#!/bin/bash

# validate cmd-line args
if [ $# -ne 1 ]; then
    echo "usage: $0 <GUI IP>"
    exit 1
fi
gui_ip=$1

source demo_common.sh

# I) Setup: Los Angeles
cmd_la="cd $DEMO_ROOT"

#   1) Download router buffer sizing bitfile
cmd_la="$cmd_la && nf2_download $BITFILE"

#   2) Fire up the software router for ARP, decap, and IP/MAC setup.
cmd_la="$cmd_la && pushd $SW_DIR && make && ./sr_go.sh && popd"

#   3) Setup event capture
cmd_la="$cmd_la && ./$EC_SETUP_SCRIPT"

#   4) Launch the router controller if the GUI is going to be used
if [ $gui_ip != "nogui" ]; then
    cmd_la="$cmd_la && pushd tomahawk && make && ./daemon_go.sh $gui_ip $GUI_EC_PORT && popd"
fi

# install cleanup handling (catch HUP, INT, TERM)
trap 'echo "cleaning up, please WAIT!" && ./demo_cleanup.sh; exit 0' 1 2 15

# II) Setup: Houston
echo "setting up the houston box at $HOU_IP ..."
#ssh root@$HOU_IP "/sbin/route add -net $TGEN_IP mss 1400 gw $LA_NF2C1_IP netmask 255.255.255.255 nf2c1 && $LA_ROUTES"


# III) Setup: traffic generator
echo "setting up the traffic generator at $TGEN_IP ..."
ssh root@$TGEN_IP "cd $DEMO_ROOT/lkm_ipip_dgu && make um && make im || make im"


# IV) Setup: GUI (local machine)
if [ $gui_ip = "nogui" ]; then
    echo "will not run the GUI ..."
else
    echo "running the GUI ..."
    java -jar BufferSizingGUI.jar &
fi


# setup la1 (the daemon command isn't returning for some reason ...)
echo "setting up the LA box at $LA_IP ..."
ssh root@$LA_IP "$cmd_la" &

# sleep until killed, then do cleanup
sleep 9999999

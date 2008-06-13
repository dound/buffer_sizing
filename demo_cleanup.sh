#!/bin/bash

# trap ctrl-c and others so we can do our work (catch HUP, INT, TERM)
trap 'echo "patience is a virtue ..."' 1 2 15

source demo_common.sh

# uninstall the kernel module
echo "cleaning up the traffic generator at $TGEN_IP ..."
ssh root@$TGEN_IP "cd $DEMO_ROOT/lkm_ipip_dgu && lsmod | grep lkm_ipip_dgu.ko && make um"

# commands to cleanup LA
kill_sr="kill `ps -A | grep $SW_NAME | cut -d\  -f1,2` 2> /dev/null"
kill_da="kill `ps -A | grep $DAEMON_NAME | cut -d\  -f1,2` 2> /dev/null"
echo "disabling event capture and cleaning up processes on the LA box at $LA_IP ..."
ssh root@$LA_IP "cd $DEMO_ROOT && ./$EC_CLEANUP_SCRIPT && $kill_sr; $kill_da"

exit 0

#!/bin/bash
set -o nounset
set -o errecho

evcap_per_info=(`echo "1 2 4 8 16 32"`)
info_per_update=(`echo "1 2 4 8 16 32 64 73"`)

echo -e "EvCaps/Info\tInfos/Update\tUpdates/sec\tBandwidth_bps"

for epi in ${evcap_per_info[@]}; do
    for ipu in ${info_per_update[@]}; do
        ./router_controller -c $epi -n $ipu -b
    done
done

echo ""
echo "Event Capture Update Rate = 32.5 updates / sec"
echo "Event Capture Bandwidth = 387 kbps"

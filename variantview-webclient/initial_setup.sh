#!/bin/bash
I2B2_WC_DIR=/var/www/html/webclient
PLUGIN_FE_DIR=$I2B2_WC_DIR/js-i2b2/cells/plugins/community
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# change i2b2_loader.js to make it load up the plugin FE
cd $I2B2_WC_DIR/js-i2b2
sudo sed -i 's@i2b2.hive.tempCellsList = \[@i2b2.hive.tempCellsList = \[\n{ code:"VARVIEW",\n  forceloading: true,\n  forceConfigMsg: { params: \[\] },\n  roles: \[ "DATA_LDS", "DATA_DEID", "DATA_PROT" \],\n  forceDir: "cells/plugins/community"\n},@' i2b2_loader.js
cd $PLUGIN_FE_DIR
sudo ln -s $DIR VARVIEW

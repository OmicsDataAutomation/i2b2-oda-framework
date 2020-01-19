#!/bin/bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PLUGIN_BE_DIR=$DIR/com.oda.i2b2.variantview
APACHE_ANT_DIR=/usr/local/i2b2/apache-ant-1.9.6/

# Stop Wildfly before compiling/deploying the plugin cell
sudo /etc/init.d/wildfly stop

# build the plugin backend
cd $PLUGIN_BE_DIR
sudo $APACHE_ANT_DIR/bin/ant -f master_build.xml build-all

# Start wildfly back up
sudo /etc/init.d/wildfly start

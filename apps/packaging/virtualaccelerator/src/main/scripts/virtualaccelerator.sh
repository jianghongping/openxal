#!/bin/bash 
CURRENT_DIR=`dirname $0`
LINKTARGET=`readlink -f $CURRENT_DIR/virtualaccelerator`
DIR=`dirname $LINKTARGET`
cd $DIR/../lib/openxal && 
java -cp "openxal.apps.virtualaccelerator-1.0.0-SNAPSHOT.jar:*" xal.app.virtualaccelerator.Main
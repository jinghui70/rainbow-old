#!/bin/sh

PROCESS=`ps -ef|grep rainbow|grep -v grep|awk '{print $2}'`

if [ -z $PROCESS ]; then
  echo "cms app is not start"
else
  kill -9 $PROCESS
fi

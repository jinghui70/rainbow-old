#!/bin/sh
# ----------------------------------------------------------------------------
# license to RAINBOW group
# ----------------------------------------------------------------------------
# ----------------------------------------------------------------------------
# RAINBOW Shutdown Batch script
#
# Required ENV vars:
# ------------------
#   JAVA_HOME - location of a JDK home dir
#   RAINBOW_HOME - location of RAINBOW installation
# ----------------------------------------------------------------------------


# resolve links - $0 may be a softlink and parse home
PRG="$0"

while [ -h "$PRG" ]; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`/"$link"
  fi
done

# Get standard environment variables
PRGDIR=`dirname "$PRG"`

# Only set RAINBOW_HOME if not already set
[ -z "$RAINBOW_HOME" ] && RAINBOW_HOME=`cd "$PRGDIR/.." ; pwd`

echo $RAINBOW_HOME

#----------------end of parse home-----------------------------

MEM_ARG="-Xms512m -Xmx1024m -Dfile.encoding=UTF-8 -Duser.language=zh -Duser.country=CN"
CLASS_PATH="$RAINBOW_HOME/lib/core.jar:$RAINBOW_HOME/lib/guava-19.0.jar:$RAINBOW_HOME/lib/fastjson-1.2.31.jar"
RAINBOWCMD="java $MEM_ARG -classpath $CLASS_PATH rainbow.core.platform.Shutdown" 
echo $RAINBOWCMD

exec $RAINBOWCMD  


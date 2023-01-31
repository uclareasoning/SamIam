#!/bin/csh
# Keith Cascio, 021005

source mypaths.csh

set INFLIBCOMPILED=$INFLIBPATH/compiled
set CPATH=$INFLIBCOMPILED

set JAVACEX="${JAVABINPATH}/javac"
set JCMD="${JAVACEX} -classpath ${CPATH}"

fgrep -q 'java5' "${1}" >/dev/null || JCMD="${JCMD} -source 1.4 -target 1.4"

echo ---------
echo Compiling $1...

$JCMD $*

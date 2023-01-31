#!/bin/csh
# Keith Cascio, 021005

source mypaths.csh

set INFLIBCOMPILED=$INFLIBPATH/compiled
set CPATH=.:$INFLIBCOMPILED

set JAVAEX="$JAVABINPATH/java"
set JCMD="$JAVAEX -classpath $CPATH"

echo -------------
echo Running class $1...

$JCMD $*

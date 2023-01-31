#!/bin/csh
# Keith Cascio, 021005

source mypaths.csh

set INFLIBCOMPILED=$INFLIBPATH/compiled
set CPATH=$INFLIBCOMPILED

set JAVAEX="$JAVABINPATH/java"

$JAVAEX -classpath $CPATH edu.ucla.util.code.Test $*

#!/bin/csh
# Keith Cascio, 032905

source mypaths.csh

set INFLIBCOMPILED=$INFLIBPATH/compiled
set CPATH=.:$INFLIBCOMPILED

set JAVAEX="$JAVABINPATH/java"
set JCMD="$JAVAEX -classpath $CPATH edu.ucla.util.code.ScriptTest"

echo Generating scripts...
# echo $JCMD $*
# $JCMD $*

foreach scriptfile ( `$JCMD $*` )
	echo $scriptfile
	chmod u+x $scriptfile
end

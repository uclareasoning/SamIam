#!/bin/bash

# compile inference test
# author keith cascio, since 20060321

if ! source mypaths.bash &> /dev/null; then
  echo 'Error: unable to find mypaths.bat'
  exit 1
fi

listfiles='files.txt';

if [ ! -r ${listfiles} ]; then
  echo "Error: ${listfiles} not found.";
  exit 1;
fi

SPATH="${INFLIBPATH:?}:.";
dirname="${INFLIBPATH:?}/compiled";
CPATH="${dirname}:${HUGINAPIJARPATH:?}";
#OTHER_OPTIONS='-g -source 1.5 -target 1.5 -Xlint:unchecked -Xlint:deprecation';
OTHER_OPTIONS='-g -source 1.5 -target 1.5';

joptions="${OTHER_OPTIONS} -sourcepath ${SPATH} -d ${dirname} -classpath ${CPATH} @${listfiles}";

if [ ! -r "${cpath}/edu/ucla/belief/BeliefNetwork.class" ]; then
  echo "Please compile inflib first";
fi

if [ ! -d ${dirname} ]; then
  echo "Error: ${dirname} not found.";
fi

cmd="${JDK15BINPATH:?}/javac ${joptions}";

echo $cmd;

$cmd;

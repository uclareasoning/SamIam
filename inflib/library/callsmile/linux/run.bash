#!/bin/bash

# test UPitt "SMILE" wrapper shared object JNI library
# author keith cascio, since 20060327

if ! source mypaths.bash &> /dev/null; then
  echo 'Error: unable to find mypaths.bash';
  exit 1;
fi

LD_LIBRARY_PATH=".${LD_LIBRARY_PATH:+:}${LD_LIBRARY_PATH:-}"; export LD_LIBRARY_PATH;

INFLIBCOMPILED=${INFLIBPATH:?}/compiled;
CPATH=${INFLIBCOMPILED};
LOCALVMARGS=${VMARGS:--Xms64m -Xmx512m};

JAVAEX=${JRE15BINPATH:?}/java;

cmd="${JAVAEX} ${LOCALVMARGS} -classpath ${CPATH} edu.ucla.belief.io.dsl.SMILEReader $*";

echo ${cmd}

${cmd}

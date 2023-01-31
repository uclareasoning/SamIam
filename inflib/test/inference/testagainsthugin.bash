#!/bin/bash

# launches inference test against hugin
# author keith cascio, since 20060321

if ! source mypaths.bash &> /dev/null; then
  echo 'Error: unable to find mypaths.bash';
  exit 1;
fi

usage="usage: $0 -m <max memory in megabytes>, e.g. $0 -m 800";

inflibcompiled="${INFLIBPATH:?}/compiled";
cpath="${inflibcompiled}:${HUGINAPIJARPATH:?}";
javaex="${JRE15BINPATH:?}/java";
libpathdef="-Djava.library.path=.:${HUGINAPIBASEPATH:?}/lib";

argmx='';
while getopts ":m:" opt; do
  case $opt in
    p  ) argmx=${OPTARG};;
    \? ) echo ${usage};
         exit 1;
  esac
done
shift $(($OPTIND - 1));

if [ -z "${argmx}" ]; then
  echo ${usage};
  exit 1;
fi

cmd="${javaex} ${libpathdef} -Xms256m -Xmx${argmx}m -classpath ${cpath} AgainstHugin $*";

echo $cmd;

$cmd;

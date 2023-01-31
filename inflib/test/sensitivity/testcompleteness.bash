#!/usr/local/bin/bash

# launches sensitivity completeness/soundness test
# author keith cascio, since 20060217

if ! source mypaths.bash &> /dev/null; then
  echo 'Error: unable to find mypaths.bash';
  exit 1;
fi

cpath="${INFLIBPATH}/compiled";
jcmd="${JRE15BINPATH}/java -Xms64m -Xmx512m -classpath ${cpath} edu.ucla.belief.sensitivity.TestCompleteness $*";

echo "$jcmd"

$jcmd

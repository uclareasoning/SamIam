#!/usr/local/bin/bash

# launches sensitivity completeness/soundness test for cancer.net
# author keith cascio, since 20060217

if ! source mypaths.bash &> /dev/null; then
  echo 'Error: unable to find mypaths.bash';
  exit 1;
fi

cmd="testcompleteness.bash -network ${NETWORKSPATH}/cancer.net -iterations 64 -soundness all,stats $*";

echo ${cmd}

${cmd}

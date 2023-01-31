#!/bin/bash

# launches inference test against hugin for cancer.net
# author keith cascio, since 20060321

if ! source mypaths.bash &> /dev/null; then
  echo 'Error: unable to find mypaths.bat'
  exit 1
fi

cmd="testagainsthugin.bash -network ${NETWORKSPATH}/cancer.oobn -iterations 2 -impl all $*";

echo $cmd;

$cmd;

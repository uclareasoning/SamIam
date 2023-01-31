#!/bin/bash
# keith cascio 20091203

source mypaths.bash &> /dev/null || {
  echo 'Did not find mypaths.bash, but '"$0"' relies on it.
(1) Create a single file named mypaths.bash based on "mypaths.template.bash" from the samiam module.
(2) Put mypaths.bash in a directory in your search path (e.g. ~/bin) so '"$0"' can find it.
(3) Edit mypaths.bash, substituting the paths as they exist on your system.'
  exit 1
}

jcmd="${JREBINPATH}/java -cp ./compiled $*"

echo $jcmd

$jcmd

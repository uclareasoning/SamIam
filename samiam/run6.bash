#!/bin/bash
# author: keith cascio, since: 20091219

source mypaths.bash &> /dev/null && {
  cpath="${INFLIBPATH}/compiled:${SAMIAMPATH}/compiled"
    exe="${JRE6BINPATH}/java"
} || {
  cpath='inflib.jar:samiam.jar'
    exe='java'
}
java_ops="-cp ${cpath} -Xms8m -Xmx512m"

[[ "$*" =~ 'edu[.]ucla' ]] || main_class_cmd='edu.ucla.belief.ui.UI nosplash'

cmd="'${exe}' ${java_ops} ${main_class_cmd} -launchcommand '${exe} ${java_ops} $*' -launchscript '$0' $*"
echo "${cmd}"
eval "${cmd}"

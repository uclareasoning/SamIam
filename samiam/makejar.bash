#!/bin/bash
# Keith Cascio 110102, 040505

source mypaths.bash &> /dev/null || {
  echo 'Did not find mypaths.bash, but '"$0"' relies on it.
(1) Create a single file named mypaths.bash based on "mypaths.template.bash" from the samiam module.
(2) Put mypaths.bash in a directory in your search path (e.g. ~/bin) so '"$0"' can find it.
(3) Edit mypaths.bash, substituting the paths as they exist on your system.'
  exit 1
}

while getopts ":vr" opt; do
  case $opt in
    v  )  verbose='true';;
    r  )  release='true';;
    \? ) echo "unknown option $opt = $OPTARG";;
  esac
done
shift $(($OPTIND - 1))

dirname='compiled'
[ -d "${dirname}/images" ] && rm -rf "${dirname}/images" &> /dev/null
[ "${release}" ] && {
  echo "For release, exporting images to ${dirname}/images ..." 1>&2
  cvs export -d "${dirname}/images" -r HEAD samiam/images >/dev/null
  extra=
} || {
  extra='images'
}

joptions='cmf'
[ "${verbose}" ] && joptions="v${joptions}"

fname='samiam.jar'
manifest='META-INF/MANIFEST.MF'
jcmd="$JDKBINPATH/jar $joptions $manifest $fname $extra -C $dirname ."

echo $jcmd
eval $jcmd


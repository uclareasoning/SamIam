#!/bin/bash
# Keith Cascio 20060324

# example sizes for inflib, -use increases disk space usage by about 50%, 10 MB
#
#> du -hs docs*
#  30M   docs   -use
#  20M   docs
#  19M   docs   -notree

if ! source mypaths.bash &> /dev/null; then
  echo 'Did not find mypaths.bash, but '"$0"' relies on it.
(1) Create a single file named mypaths.bash based on "mypaths.template.bash" from the samiam module.
(2) Put mypaths.bash in a directory in your search path (e.g. ~/bin) so '"$0"' can find it.
(3) Edit mypaths.bash, substituting the paths as they exist on your system.'
  exit 1
fi

fmanifest='META-INF/MANIFEST.MF'
[ -f "${fmanifest}" ] || {
  echo "Missing required file ${fmanifest}" 1>&2
  exit 9
}

verbosity='-quiet'
while getopts ":uv" opt; do
  case $opt in
    u  )    useuse='-use';     echo 'Passing -use...    ' 1>&2;;
    v  ) verbosity='-verbose'; echo 'Passing -verbose...' 1>&2;;
    \? ) echo "unknown option $opt = $OPTARG";;
  esac
done
shift $(($OPTIND - 1))

type gsed &>/dev/null && SED=gsed || SED=sed
IMPLEMENTATIONVERSION=$(${SED} -nre 's/^.*\<Implementation-Version:[[:space:]]+([0-9.]+)[^0-9.]*$/\1/p' "${fmanifest}")
                  NOW=$(date '+%Y-%m-%d %H:%M:%S')
                 YEAR=$(date '+%Y')
                tight="INFLIB v${IMPLEMENTATIONVERSION} Public API at ${NOW}"

LINKS='-link "http://java.sun.com/javase/6/docs/api"'
TITLEZ="-doctitle '${tight}' -windowtitle '${tight}' -bottom 'Copyright ${YEAR} <a href='http://reasoning.cs.ucla.edu'>UCLA Automated Reasoning Group</a>'"

options="${verbosity} ${useuse} -public -author -nosince -noqualifier all -nohelp ${LINKS} ${TITLEZ}"

dirname="docs"
cpath="${INFLIBPATH}/compiled"
jcmd="${JDK6BINPATH}/javadoc ${options} -d ${dirname} -classpath ${cpath} @files.txt @files5.txt"

if [ ! -d $dirname ]; then
  if [ -r $dirname ]; then
    echo "Please remove file \"$dirname\"."
    exit 1
  else
    echo "Creating \"$dirname\" directory."
    mkdir $dirname
  fi
fi

echo "$jcmd"
eval "$jcmd" && {
  echo 'Settings file system permissions...' 1>&2
  [[ $(uname) =~ 'Linux' ]] && {
    chmod -R ugo+rX "${dirname}"
  } || {
    chmod -R ugo+r  "${dirname}"
    /usr/bin/find   "${dirname}" -type d -exec chmod ugo+rx {} \;
  }
}


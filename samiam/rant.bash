#!/bin/bash
# Keith Cascio 050605
#   "rant" means "run ant"
#   Script calls Apache Ant - a Java-based build tool,
#   defining relevant variables from mypaths.bash
#   Use command-line option "-projecthelp"
#   to see a list of build targets

if ! source mypaths.bash &> /dev/null; then
        echo 'Did not find mypaths.bash, but '"$0"' relies on it.'
        echo '(1) Create a single file named mypaths.bash based on "mypaths.template.bash" from the samiam module.'
        echo '(2) Put mypaths.bash in a directory in your search path (e.g. ~/bin) so '"$0"' can find it.'
        echo '(3) Edit mypaths.bash, substituting the paths as they exist on your system.'
        exit 1
fi

REQUIRED_VAR_NAMES='INFLIBPATH SAMIAMPATH BATCHPATH JRE14BOOTCLASSPATH'

undefinederror()
{
  echo "***** Error: undefined variable \"$1\" *****";
  echo "    define { ${REQUIRED_VAR_NAMES} } in mypaths.bash";
  exit 1;
}

for varname in ${REQUIRED_VAR_NAMES}; do
  # echo '  testing defined "$'"$varname"' ...';

  #    varvalue="${!varname}";    #          <---- new style uses ${!x}
  eval varvalue='${'${varname}'}';#          <---- old style uses eval

  if [ -z "${varvalue}" ]; then
    undefinederror ${varname};
  else
    cmdlineproperties="${cmdlineproperties} \"-D${varname}=${varvalue}\"";
  fi
done

ant ${cmdlineproperties} -buildfile build.xml $*

#!/bin/bash

# author keith cascio, since 20060708
# build script for AgainstHugin.cpp, a native hugin calculator program

if ! source mypaths.bash &> /dev/null; then
  echo 'Error: unable to find mypaths.bat';
  exit 1;
fi
pathtohugin="${HUGINAPIBASEPATH:?}";

doBuild()
{
  local caption=$1;
  local nameLibrary=$2;
  local nameExec=$3;
  local nameObject=$4;
  local macros=$5;

  echo "building ${caption}-precision hugin calculator...";
  cmdcompile="g++ -g3 -fexceptions ${macros} -I${pathtohugin}/include -c AgainstHugin.cpp -o ${nameObject}";
  echo ${cmdcompile};
  ${cmdcompile};
  cmdlink="g++    -g3 -fexceptions ${macros} -L${pathtohugin}/lib -l${nameLibrary} ${nameObject} -o ${nameExec}";
  echo ${cmdlink};
  ${cmdlink};
}

argprecision='double';
while getopts ":p:" opt; do
  case $opt in
    p  ) argprecision=${OPTARG};;
    \? ) echo 'usage: $0 [-p <double|single|both>, default double]';
         exit 1;
  esac
done
shift $(($OPTIND - 1));

case ${argprecision} in
  double   ) buildDouble=true;;
  single   ) buildSingle=true;;
  all|both ) buildDouble=true;  buildSingle=true;;
esac

if [ -n "$buildDouble" ]; then
  doBuild 'double' 'hugincpp2' 'hugindouble' 'AgainstHuginDouble.o' '-DH_DOUBLE';
fi

if [ -n "$buildSingle" ]; then
  doBuild 'single' 'hugincpp' 'huginsingle' 'AgainstHuginSingle.o' '';
fi

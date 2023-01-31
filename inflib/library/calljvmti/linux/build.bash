#!/bin/bash
#*******************************************************************************
# Copyright (c) 2000, 2005 IBM Corporation and others.
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
#
# Contributors:
#     IBM Corporation - initial API and implementation
#     Kevin Cornell (Rational Software Corporation)
#     keith cascio 20060509 (UCLA Automated Reasoning Group)
#*******************************************************************************
#
# Usage: sh build.sh [<optional switches>] [clean]
#
#   where the optional switches are:
#       -output <PROGRAM_OUTPUT>  - executable filename ("samiam")
#       -os     <DEFAULT_OS>      - default Eclipse "-os" value
#       -arch   <DEFAULT_OS_ARCH> - default Eclipse "-arch" value
#       -ws     <DEFAULT_WS>      - default Eclipse "-ws" value
#
#
#    This script can also be invoked with the "clean" argument.

if ! source mypaths.bash &> /dev/null; then
  echo 'Did not find mypaths.bash, but '"$0"' relies on it.'
  echo '(1) Create a single file named mypaths.bash based on "mypaths.template.bash" from the samiam module.'
  echo '(2) Put mypaths.bash in a directory in your search path (e.g. ~/bin) so '"$0"' can find it.'
  echo '(3) Edit mypaths.bash, substituting the paths as they exist on your system.'
  exit 1
fi
foo="${JDK15BASEPATH:?}";
export JDK15BASEPATH;

cd `dirname $0`

# Define default values for environment variables used in the makefiles.
programOutput="libcalljvmti.so"
defaultOS=""
defaultOSArch=""
defaultWS="gtk"
makefile=""
if [ "$OS" = "" ];  then
    OS=`uname -s`
fi
if [ "$MODEL" = "" ];  then
    MODEL=`uname -m`
fi

case $OS in
  "Linux")
    makefile="make_linux.mak"
    defaultOS="linux"
    case $MODEL in
      "x86_64")
        defaultOSArch="x86_64"
        OUTPUT_DIR="../../bin/$defaultWS/$defaultOS/$defaultOSArch"
        ;;
      i?86)
        defaultOSArch="x86"
        OUTPUT_DIR="../../bin/$defaultWS/$defaultOS/$defaultOSArch"
        ;;
      "ppc")
        defaultOSArch="ppc"
        OUTPUT_DIR="../../bin/$defaultWS/$defaultOS/$defaultOSArch"
        ;;
      "ppc64")
        defaultOSArch="ppc64"
        OUTPUT_DIR="../../bin/$defaultWS/$defaultOS/$defaultOSArch"
        ;;
      "ia64")
        defaultOSArch="ia64"
        OUTPUT_DIR="../../bin/$defaultWS/$defaultOS/$defaultOSArch"
        ;;
      *)
        echo "*** Unknown MODEL <${MODEL}>"
        ;;
    esac
    ;;
  "SunOS")
    makefile="make_solaris.mak"
    defaultOS="solaris"
    defaultOSArch="sparc"
    OUTPUT_DIR="../../bin/$defaultWS/$defaultOS/$defaultOSArch"
    PATH=/usr/ccs/bin:/usr/local/bin:$PATH
    export PATH
    ;;
  *)
  echo "Unknown OS -- build aborted"
  ;;
esac

# Parse the command line arguments and override the default values.
extraArgs=""
while [ "$1" != "" ]; do
    if [ "$1" = "-os" ] && [ "$2" != "" ]; then
        defaultOS="$2"
        shift
    elif [ "$1" = "-arch" ] && [ "$2" != "" ]; then
        defaultOSArch="$2"
        shift
    elif [ "$1" = "-ws" ] && [ "$2" != "" ]; then
        defaultWS="$2"
        shift
    elif [ "$1" = "-output" ] && [ "$2" != "" ]; then
        programOutput="$2"
        shift
    else
        extraArgs="$extraArgs $1"
    fi
    shift
done

# Set up environment variables needed by the makefiles.
PROGRAM_OUTPUT="$programOutput"
DEFAULT_OS="$defaultOS"
DEFAULT_OS_ARCH="$defaultOSArch"
DEFAULT_WS="$defaultWS"

export OUTPUT_DIR PROGRAM_OUTPUT DEFAULT_OS DEFAULT_OS_ARCH DEFAULT_WS

# 2009-01-06
# STDERR of `make all` redirected to a sed filter because we did not
# find a way to tell the linker
# "fail on unresolved symbol references except for GetCurrentThreadCpuTime".
# GetCurrentThreadCpuTime etc come from JRE at dynamic link time.
#
#     make -f $makefile all   2> >(sed -re '/In function/N;/warning.*undefined reference.*((Get|Add)(CurrentThreadCpuTime|Env|Capabilities))/c \
# Benign warning suppressed.')

# If the OS is supported (a makefile exists)
if [ "$makefile" != "" ]; then
  if [ "$extraArgs" != "" ]; then
    make -f $makefile $extraArgs
  else
    echo "Building $OS JVMTI access shared object JNI library. Defaults: -os $DEFAULT_OS -arch $DEFAULT_OS_ARCH -ws $DEFAULT_WS"
    make -f $makefile clean
    make -f $makefile all
  fi
  if [ -r "$programOutput" ]; then
    chmod ugo+r "$programOutput";
  fi
else
  echo "Unknown OS ($OS) -- build aborted"
fi

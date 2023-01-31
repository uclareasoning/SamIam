#!/bin/bash
# Keith Cascio 103102, 021105, 040505, 20060525

if ! source mypaths.bash &> /dev/null; then
        echo 'Did not find mypaths.bash, but '"$0"' relies on it.'
        echo '(1) Create a single file named mypaths.bash based on "mypaths.template.bash" from the samiam module.'
        echo '(2) Put mypaths.bash in a directory in your search path (e.g. ~/bin) so '"$0"' can find it.'
        echo '(3) Edit mypaths.bash, substituting the paths as they exist on your system.'
        exit 1
fi

if [ -n "${JDK15BINPATH}" ]; then
  JAVAC="${JDK15BINPATH}/javac";
else
  JAVAC="${JDKBINPATH}/javac";
fi

dirname='compiled';
j4cmd="${JAVAC} -source 1.4 -target 1.4 -d ${dirname}                       @files.txt";
j5cmd="${JAVAC} -source 1.5 -target 1.5 -d ${dirname} -classpath ${dirname} @files5.txt";

if [ ! -d $dirname ]; then
  if [ -r $dirname ]; then
    echo "Please remove file \"$dirname\".";
    exit 1;
  else
    echo "Creating \"$dirname\" directory.";
    mkdir $dirname;
  fi
fi

echo "$j4cmd";

if $j4cmd; then
  if [ -n "${JDK15BINPATH}" ]; then
    echo "$j5cmd";
    $j5cmd;
  fi
  ./makejar.bash;
  echo 'Done compiling inflib.';
else
  echo 'Failed compiling inflib.';
  exit 1;
fi

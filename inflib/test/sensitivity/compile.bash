#!/usr/local/bin/bash
# keith cascio 103102, 021105, 040505, 20060217

if ! source mypaths.bash &> /dev/null; then
  echo 'Error: unable to find mypaths.bat'
  exit 1
fi

dirname="${INFLIBPATH}/compiled"

if [ ! -r "${dirname}/edu/ucla/belief/BeliefNetwork.class" ]; then
  echo "Error: please compile inflib first!";
  exit 1;
fi

listfiles=files.txt
listfilesexpanded=filesexpanded.txt

if [ -r ${listfiles} ]; then
  sed -e 's#^.*$#'"${INFLIBPATH}"'/&#' ${listfiles} >| ${listfilesexpanded};
else
  echo "Error: ${listfiles} not found."
  exit 1;
fi

jcmd="${JDK15BINPATH}/javac -source 1.5 -target 1.5 -sourcepath ${INFLIBPATH} -classpath ${dirname} -d ${dirname} @${listfilesexpanded}"

if [ ! -d $dirname ]; then
  if [ -r $dirname ]; then
    echo "Please remove file \"$dirname\".";
    exit 1;
  else
    echo "Creating directory \"$dirname\".";
    mkdir $dirname;
  fi
fi

echo "$jcmd"

if $jcmd; then
  echo "Done compiling sensitivity tests.";
else
  echo "Failed compiling sensitivity tests.";
  exit 1;
fi

#!/bin/bash
# Keith Cascio 041102, 040505

if ! source mypaths.bash &> /dev/null; then
        echo 'Did not find mypaths.bash, but '"$0"' relies on it.'
        echo '(1) Create a single file named mypaths.bash based on "mypaths.template.bash" from the samiam module.'
        echo '(2) Put mypaths.bash in a directory in your search path (e.g. ~/bin) so '"$0"' can find it.'
        echo '(3) Edit mypaths.bash, substituting the paths as they exist on your system.'
        exit 1
fi

destinationdirname=docs

echo "Deposit html in directory \"$destinationdirname\"?"
read answer_dir

if( test $answer_dir = n ) then
 newdirname=

 while( test -z "$newdirname" ) do
  echo "Type new root directory (or quit)."
  read newdirname
  if( test $newdirname = quit ) then
   exit 0
  fi
  if( test -d "./$newdirname" ) then
   newdirname="./$newdirname"
  fi
  if( test -d "$newdirname" ) then
   echo "\"$newdirname\" exists, clean and overwrite?"
   read answer_overwrite
   if( test $answer_overwrite = y ) then
    echo "Cleaning \"$newdirname\"..."
    if( ! rm -fR $newdirname/* ) then
     echo "Failed to clean \"$newdirname\""
     exit 1
    fi
   else
    newdirname=
   fi
  elif( test -e "$newdirname" ) then
   echo "Error: \"$newdirname\" is not a directory."
   exit 1
  fi
 done

 destinationdirname=$newdirname
fi

jcmd="$JDKBINPATH/javadoc -author -public -windowtitle INFLIB -nohelp -verbose -d $destinationdirname -classpath . -subpackages edu:il2"

echo "Writing html to \"$destinationdirname\"..."
echo $jcmd
$jcmd

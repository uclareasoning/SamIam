#!/bin/bash
# Keith Cascio 110102, 021105, 040505

if ! source mypaths.bash &> /dev/null; then
        echo 'Did not find mypaths.bash, but '"$0"' relies on it.'
        echo '(1) Create a single file named mypaths.bash based on "mypaths.template.bash" from the samiam module.'
        echo '(2) Put mypaths.bash in a directory in your search path (e.g. ~/bin) so '"$0"' can find it.'
        echo '(3) Edit mypaths.bash, substituting the paths as they exist on your system.'
        exit 1
fi

#joptions=vcmf
joptions='cmf';

dirname='compiled';
fname='inflib.jar';
manifest='META-INF/MANIFEST.MF';
resources='edu/ucla/belief/io/xmlbif/bif.xsd';
jcmd="$JDKBINPATH/bin/jar $joptions $manifest $fname $resources -C $dirname ."

echo $jcmd

$jcmd

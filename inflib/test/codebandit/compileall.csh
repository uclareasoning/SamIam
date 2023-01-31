#!/bin/csh
# Keith Cascio, 021005

foreach javafile ( `cat javafiles.testdata` )
	compileone.csh $* $javafile
end

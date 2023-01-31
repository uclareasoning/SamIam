#!/bin/csh
# Keith Cascio, 021005

foreach classfile ( `cat classfiles.testdata` )
	runone.csh $classfile $*
end

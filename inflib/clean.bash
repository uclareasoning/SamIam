#!/bin/bash
# Keith Cascio 040605

effort=0

if [ -d compiled/edu ]; then
	rm -r compiled/edu
	effort=1
fi

if [ -d compiled/il2 ]; then
	rm -r compiled/il2
	effort=1
fi

if [ $effort -eq 0 ]; then
	echo "inflib was already clean.";
else
	echo "Done cleaning inflib."
fi

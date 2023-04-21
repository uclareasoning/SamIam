#!/bin/sh 

libtoolize --force
aclocal

#cat /usr/keith/libtool/1.5.2/share/aclocal/libtool.m4 >> aclocal.m4

LIBTOOLINCLUDES="ltsugar.m4 ltversion.m4 ltoptions.m4 argz.m4 ltdl.m4 libtool.m4"

SEDEXPRESSION='s/\/libtool$//g'
LIBTOOLBINDIR=`which libtool | sed -e "$SEDEXPRESSION"`
LIBTOOLINSTALLDIR=`echo "$LIBTOOLBINDIR" | sed -e "s/\/bin//g"`
LIBTOOLACLOCALDIR="$LIBTOOLINSTALLDIR/share/aclocal"

if test -f "$LIBTOOLACLOCALDIR/libtool.m4"; then
  for incfile in $LIBTOOLINCLUDES
  do
    INCPATH="$LIBTOOLACLOCALDIR/$incfile"
    if test -f "$INCPATH"; then
     echo "cat \"$INCPATH\" >> aclocal.m4"
     cat "$INCPATH" >> aclocal.m4
    fi
  done
else
  echo "unable to find libtool.m4, dne at $LIBTOOLACLOCALDIR/libtool.m4"
  exit 1
fi

autoconf
autoheader 
automake --foreign --add-missing

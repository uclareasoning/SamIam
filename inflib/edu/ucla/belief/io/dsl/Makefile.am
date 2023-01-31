#old version
#noinst_LTLIBRARIES = libcallsmile.la
#libcallsmile_la_SOURCES = SMILEReader.cpp
#libcallsmile_la_LIBADD = /home/keith/smile/libsmilexml.a /home/keith/smile/libsmile.a @INTLLIBS@
#libcallsmile_la_LDFLAGS = -L/home/keith/smile -Xlinker -zdefs -no-undefined
##libcallsmile_la_LDADD = @INTLLIBS@ libsmile.a
#INCLUDES = -I/home/keith/smile -I/usr/local/j2sdk1.4.1_01/include -I/usr/local/j2sdk1.4.1_01/include/linux


CFLAGS = @CFLAGS@ @WARNS@
LDADD = libcallsmile.la

lib_LTLIBRARIES = libcallsmile.la
libcallsmile_la_SOURCES = SMILEReader.cpp
libcallsmile_la_LDFLAGS = -version-info 1:0:0 -Xlinker -zdefs -no-undefined

# variables set in smile.m4 and java.m4
libcallsmile_la_LIBADD = @LIBSMILEXML@ @LIBSMILE@
INCLUDES = @SMILE_INCLUDES@ @JNI_INCLUDES@

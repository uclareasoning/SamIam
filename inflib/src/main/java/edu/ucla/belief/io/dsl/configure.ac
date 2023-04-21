#                                               -*- Autoconf -*-
# Process this file with autoconf to produce a configure script.

AC_PREREQ(2.59)
AC_INIT([callsmile extension library for SamIam 2.2], [1.1], [samiam@cs.ucla.edu], [callsmile])
AC_CONFIG_SRCDIR([edu_ucla_belief_io_dsl_SMILEReader.h])
AC_CONFIG_HEADERS([config.h])
# AC_CONFIG_HEADER([config.h])
# AM_CONFIG_HEADER(config.h)
AM_INIT_AUTOMAKE([1.6])

sinclude(smile.m4)
sinclude(java.m4)

# Checks for programs.
AC_PROG_CXX([g++-3.3.2])
AC_PROG_CC
AC_DISABLE_STATIC
AC_PROG_LIBTOOL
#AC_PROG_RANLIB obsoleted by above

# Checks for libraries.
#AC_CHECK_FILE([libsmile.a])
ARGROUP_CHECK_SMILE

# Checks for header files.
#AC_CHECK_HEADERS([jni.h],[],[AC_MSG_ERROR([Error! jni.h not found.])])
SVN_CHECK_JDK

# Checks for typedefs, structures, and compiler characteristics.
AC_C_CONST
AC_HEADER_STDBOOL

# Checks for library functions.

AC_CONFIG_FILES([Makefile])
AC_OUTPUT

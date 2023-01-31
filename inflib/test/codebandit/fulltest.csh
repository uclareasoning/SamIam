#!/bin/csh
# Keith Cascio, 021005

echo -------------------------
echo -------------------------
echo Phase 1 - Code Generation
writecode.csh $*

#if not ERRORLEVEL 0 goto end

echo ---------------------
echo ---------------------
echo Phase 2 - Compilation
compileall.csh

#if not ERRORLEVEL 0 goto end

echo -------------
echo -------------
echo Phase 3 - Run
runall.csh

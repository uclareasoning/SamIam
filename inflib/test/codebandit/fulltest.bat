@echo off

echo -------------------------
echo -------------------------
echo Phase 1 - Code Generation
call writecode.bat %*

if not ERRORLEVEL 0 goto end

echo ---------------------
echo ---------------------
echo Phase 2 - Compilation
call compileall.bat

if not ERRORLEVEL 0 goto end

echo -------------
echo -------------
echo Phase 3 - Run
call runall.bat

:end

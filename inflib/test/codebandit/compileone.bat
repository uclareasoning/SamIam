@echo off

setlocal
call mypaths.bat > nul 2>&1
if errorlevel 1 goto error_missing_mypaths

set INFLIBCOMPILED=%INFLIBPATH%\compiled
set CPATH=%INFLIBCOMPILED%

set JAVACEX="%JDKBINPATH%\javac.exe"
set JCMD=%JAVACEX% -classpath %CPATH%

find "java5" %1 >nul
if errorlevel 1 set JCMD=%JCMD% -source 1.4 -target 1.4

echo ---------
echo Compiling %1...

rem @echo on

%JCMD% %*
goto end

:error_missing_mypaths
  echo Did not find mypaths.bat, but "%0" relies on it.
  echo (1) Create a single file named mypaths.bat based on "mypaths.template.bat" from the samiam module.
  echo (2) Put mypaths.bat in a directory in your command path (e.g. ~/bin) so "%0" can find it.
  echo (3) Edit mypaths.bat, substituting the paths as they exist on your system.
goto end

:end
endlocal

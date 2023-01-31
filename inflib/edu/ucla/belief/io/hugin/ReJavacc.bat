@echo off
rem author keith cascio, since 20060518

vol >nul 2>&1
call mypaths.bat >nul 2>&1
if errorlevel 1 goto error_missing_mypaths

call "%JAVACCBASEPATH%\bin\javacc.bat" -STATIC:false %*
if exist ParseException.java   del ParseException.java
if exist SimpleCharStream.java del SimpleCharStream.java
if exist Token.java            del Token.java
if exist TokenMgrError.java    del TokenMgrError.java
goto :eof

:error_missing_mypaths
  echo Did not find mypaths.bat, but "%0" relies on it.
  echo (1) Create a single file named mypaths.bat based on "mypaths.template.bat" from the samiam module.
  echo (2) Put mypaths.bat in a directory in your command path (e.g. ~/bin) so "%0" can find it.
  echo (3) Edit mypaths.bat, substituting the paths as they exist on your system.
goto :eof

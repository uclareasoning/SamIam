@echo off
rem author: keith cascio, since: 20071204

setlocal
vol >nul 2>&1
call mypaths.bat >nul 2>&1
if errorlevel 1 goto error_missing_mypaths

set CPATH=%INFLIBPATH%\compiled
set CPATH=%CPATH%;%SAMIAMPATH%\compiled
set JAVA_OPS=-cp %CPATH%

echo %* | find "edu.ucla" >nul
if errorlevel 1 set MAIN_CLASS_CMD=edu.ucla.belief.ui.UI nosplash

@echo on
"%JRE6BINPATH%\java.exe" %JAVA_OPS% %MAIN_CLASS_CMD% %*
@echo off
goto end

:error_missing_mypaths
  echo Did not find mypaths.bat, but "%0" relies on it.
  echo (1) Create a single file named mypaths.bat based on "mypaths.template.bat" from the samiam module.
  echo (2) Put mypaths.bat in a directory in your command path (e.g. ~/bin) so "%0" can find it.
  echo (3) Edit mypaths.bat, substituting the paths as they exist on your system.
goto end

:end
endlocal

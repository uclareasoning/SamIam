@echo off
setlocal
call mypaths.bat 2>&1 | find "not recognized" 2>&1 > nul
if errorlevel 1 goto success_mypaths
goto error_missing_mypaths
:success_mypaths

set CPATH=%INFLIBPATH%\compiled
@echo on
"%JDKBINPATH%\serialver.exe" -classpath "%CPATH%" -show
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

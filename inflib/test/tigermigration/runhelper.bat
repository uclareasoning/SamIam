@echo off
setlocal
call mypaths.bat > nul 2>&1
if errorlevel 1 goto error_missing_mypaths

set CPATH=.\compiled
set CPATH=%CPATH%;%INFLIBPATH%\compiled
set CPATH=%CPATH%;%SAMIAMPATH%\compiled
set SPATHS=-sourcepath "%INFLIBPATH%" -sourcepath "%SAMIAMPATH%" -sourcepath "C:\keith\program files\Java\jdk1.5.0\src"

@echo on
@rem "%JDK15BINPATH%\javac.exe" -classpath %CPATH% -sourcepath %SAMIAMPATH% -d %SAMIAMPATH%\compiled edu\ucla\util\SignatureHelper.java
"%JRE15BINPATH%\java.exe" -cp %CPATH% signature.SignatureHelper %SPATHS% %*
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

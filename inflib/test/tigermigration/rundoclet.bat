@echo off
setlocal
call mypaths.bat > nul 2>&1
if errorlevel 1 goto error_missing_mypaths

set DOCLETPATH=.\compiled
rem set CPATH=%DOCLETPATH%
rem set CPATH=%CPATH%;%INFLIBPATH%\compiled
rem set CPATH=%CPATH%;%SAMIAMPATH%\compiled

rem set SPATHS=-sourcepath "%INFLIBPATH%" -sourcepath "%SAMIAMPATH%" -sourcepath "C:\keith\program files\Java\jdk1.5.0\src"
set SPATH=%INFLIBPATH%;%SAMIAMPATH%
set SPATH=%SPATH%;C:\keith\program files\Java\jdk1.5.0\src

rem set PACKAGES=edu.ucla.util
set PACKAGES=edu.ucla.belief edu.ucla.structure java.util java.lang

@echo on
"%JDK15BINPATH%\javadoc.exe" -source 1.5 -sourcepath "%SPATH%" %PACKAGES% -docletpath "%DOCLETPATH%" -doclet signature.SignatureDoclet %*
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

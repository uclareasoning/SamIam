@echo off
rem Keith Cascio 050605
rem   "rant" means "run ant"
rem   Script calls Apache Ant - a Java-based build tool,
rem   defining relevant variables from mypaths.bat
rem   Use command-line option "-projecthelp"
rem   to see a list of build targets

setlocal

call mypaths.bat > nul 2>&1
if errorlevel 1 goto error_missing_mypaths

set REQUIRED_VAR_NAMES=INFLIBPATH SAMIAMPATH BATCHPATH JRE14BOOTCLASSPATH
set cmdlineproperties=

for %%n in (%REQUIRED_VAR_NAMES%) do if not defined %%n (
  call :error_missing_variable "%%n" & goto end
) else (
  call :concatenate "-D%%n=%%%%n%%"
)

:do_ant
ant %cmdlineproperties% -buildfile build.xml %*
goto end

:error_missing_mypaths
  echo Did not find mypaths.bat, but "%0" relies on it.
  echo (1) Create a single file named mypaths.bat based on "mypaths.template.bat" from the samiam module.
  echo (2) Put mypaths.bat in a directory in your command path (e.g. ~/bin) so "%0" can find it.
  echo (3) Edit mypaths.bat, substituting the paths as they exist on your system.
goto end

:error_missing_variable
echo ***** Error: undefined variable %1 *****
echo     define { %REQUIRED_VAR_NAMES% } in mypaths.bat
goto :eof

:concatenate
set cmdlineproperties=%cmdlineproperties% %1
goto :eof

:end
endlocal

rem echo nul | echo %%%%n%%
rem echo nul | set varvalue=%%%%n%%
rem (set varvalue=%%n%) & (echo %varvalue%)

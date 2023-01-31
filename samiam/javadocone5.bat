@echo off
rem author: keith cascio, since: 20041024
rem This program requires one argument: the full path to the file to compile.
rem Any options passed before the file path will be passed on to javadoc

:doshift
if not "%2"=="" (
  rem echo shifting "%2"
  shift
  goto doshift
)

set filepath=%1

setlocal
call mypaths.bat > nul 2>&1
if errorlevel 1 goto error_missing_mypaths

set INFLIBCOMPILEDPATH=%INFLIBPATH%\compiled
set SAMIAMCOMPILEDPATH=%SAMIAMPATH%\compiled
set BATCHCOMPILEDPATH=%BATCHPATH%\compiled
set PRIMULACOMPILEDPATH=%PRIMULAPATH%\compiled

rem set OTHER_OPTIONS=-g

echo %filepath% | find /i /c "\inflib\" > NUL
if errorlevel 1 (
	echo %filepath% | find /i /c "\samiam\" > NUL
	if errorlevel 1 (
		echo %filepath% | find /i /c "\batch\" > NUL
		if errorlevel 1 (
			echo %filepath% | find /i /c "\primula\" > NUL
			if errorlevel 1 (
				echo Error cannot identify input file.
				goto end
			) else (
				goto found_primula
			)
		) else goto found_batch
	) else goto found_samiam
) else goto found_inflib

:found_samiam
echo Javadocing samiam file...
set CPATH=%INFLIBCOMPILEDPATH%;%SAMIAMCOMPILEDPATH%
set dirname=%SAMIAMPATH%\docs
goto setoptions

:found_inflib
echo Javadocing inflib file...
set CPATH=%INFLIBCOMPILEDPATH%
set dirname=%INFLIBPATH%\docs
goto setoptions

:found_batch
echo Javadocing batchtool file...
set CPATH=%BATCHCOMPILEDPATH%;%INFLIBCOMPILEDPATH%;%SAMIAMCOMPILEDPATH%
set dirname=%BATCHPATH%\docs
goto setoptions

:found_primula
echo Javadocing primula file...
set CPATH=%PRIMULACOMPILEDPATH%
set dirname=%PRIMULAPATH%\docs
goto setoptions

:setoptions
echo %* | find /i /c "-private" > NUL
if errorlevel 1 (
  echo.
  echo ******************************************************************************
  echo     NOTE: Will not document private methods.
  echo     Please specify command-line option '-private' to document private methods.
  echo ******************************************************************************
  echo.
)
set joptions=%OTHER_OPTIONS% -d %dirname% -classpath %CPATH% -author -link "http://java.sun.com/javase/6/docs/api"

:compile
echo %JDK15BINPATH%\javadoc.exe %joptions% %*
"%JDK15BINPATH%\javadoc.exe" %joptions% %filepath%
if not ERRORLEVEL 1 goto openhtml
goto end

:error_missing_mypaths
  echo Did not find mypaths.bat, but "%0" relies on it.
  echo (1) Create a single file named mypaths.bat based on "mypaths.template.bat" from the samiam module.
  echo (2) Put mypaths.bat in a directory in your command path (e.g. ~/bin) so "%0" can find it.
  echo (3) Edit mypaths.bat, substituting the paths as they exist on your system.
goto end

:openhtml
start %dirname%\index.html
goto end

:end
endlocal

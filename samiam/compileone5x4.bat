@echo off
rem Keith Cascio 030204
rem This program requires one argument: the full path to the file to compile.
rem Second argument optional: full path to folder containing file.

set filepath=%1
set filefolder=%2

setlocal
call mypaths.bat > nul 2>&1
if errorlevel 1 goto error_missing_mypaths

set INFLIBCOMPILEDPATH=%INFLIBPATH%\compiled
set SAMIAMCOMPILEDPATH=%SAMIAMPATH%\compiled
set BATCHCOMPILEDPATH=%BATCHPATH%\compiled
set PRIMULACOMPILEDPATH=%PRIMULAPATH%\compiled

set OTHER_OPTIONS=-g -source 1.4 -target 1.4 -bootclasspath "%JRE14BOOTCLASSPATH%"

echo %filepath% | find /i /c "\inflib\" > NUL
if errorlevel 1 (
	echo %filepath% | find /i /c "\samiam\" > NUL
	if errorlevel 1 (
		echo %filepath% | find /i /c "\batch\" > NUL
		if errorlevel 1 (
			echo %filepath% | find /i /c "\primula\" > NUL
			if errorlevel 1 (
				rem echo Error cannot identify input file.
				goto found_non_packaged
			) else (
				goto found_primula
			)
		) else goto found_batch
	) else goto found_samiam
) else goto found_inflib

:found_non_packaged
echo Compiling non-packaged file...
set CPATH=%filefolder%
set dirname=%filefolder%
goto setoptions

:found_samiam
echo Compiling samiam file...
set CPATH=%INFLIBCOMPILEDPATH%;%SAMIAMCOMPILEDPATH%
set dirname=%SAMIAMCOMPILEDPATH%
goto setoptions

:found_inflib
echo Compiling inflib file...
set CPATH=%INFLIBCOMPILEDPATH%
set dirname=%INFLIBCOMPILEDPATH%
goto setoptions

:found_batch
echo Compiling batchtool file...
set CPATH=%BATCHCOMPILEDPATH%;%INFLIBCOMPILEDPATH%;%SAMIAMCOMPILEDPATH%
set dirname=%BATCHCOMPILEDPATH%
goto setoptions

:found_primula
echo Compiling primula file...
set CPATH=%PRIMULACOMPILEDPATH%
set dirname=%PRIMULACOMPILEDPATH%
goto setoptions

:setoptions
set joptions=%OTHER_OPTIONS% -d %dirname% -classpath %CPATH%

:compile
echo %JDK15BINPATH%\javac.exe %joptions% %filepath%
"%JDK15BINPATH%\javac.exe" %joptions% %filepath%
goto end

:error_missing_mypaths
  echo Did not find mypaths.bat, but "%0" relies on it.
  echo (1) Create a single file named mypaths.bat based on "mypaths.template.bat" from the samiam module.
  echo (2) Put mypaths.bat in a directory in your command path (e.g. ~/bin) so "%0" can find it.
  echo (3) Edit mypaths.bat, substituting the paths as they exist on your system.
goto end

:end
endlocal

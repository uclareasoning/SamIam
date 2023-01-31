@echo off
REM Keith Cascio 110102
REM Sean Soria 052103

set joptions=cmf

set dirname=compiled
set fname=samiam.jar
set manifest=META-INF/MANIFEST.MF
echo Creating %fname%...
jar %joptions% %manifest% %fname% images -C %dirname% .
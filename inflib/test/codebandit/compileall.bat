@echo off

for /F %%f in ( javafiles.testdata ) do @call compileone.bat %* %%f
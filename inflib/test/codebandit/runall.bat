@echo off

for /F %%f in ( classfiles.testdata ) do @call runone.bat %%f %*

@echo off
setlocal

gradle --no-daemon clean build
if errorlevel 1 exit /b 1

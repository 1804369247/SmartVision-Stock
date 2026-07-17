@echo off
cd /d F:\SmartVision Stock\backend
set JAVA_HOME=C:\Users\18043\AppData\Local\Programs\Java\jdk-21
set PATH=%JAVA_HOME%\bin;%PATH%
C:\Maven\apache-maven-3.9.6\bin\mvn.cmd -o package -DskipTests
echo BUILD_EXIT_CODE=%ERRORLEVEL% > "F:\SmartVision Stock\backend\.build_exit"
exit /b %ERRORLEVEL%

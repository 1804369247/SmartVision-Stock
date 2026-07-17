@echo off
REM Maven Wrapper for Windows - Direct Java invocation using bundled Maven 3.9.9
REM Falls back to managed JDK if JAVA_HOME is not set

set "MVN_HOME=%~dp0apache-maven-3.9.9"

if not exist "%MVN_HOME%\boot\plexus-classworlds-2.8.0.jar" (
    echo ERROR: Maven 3.9.9 not found at %MVN_HOME%
    exit /b 1
)

REM Use JAVA_HOME if set, otherwise use managed JDK
set "JAVACMD="
if not "%JAVA_HOME%"=="" (
    set "JAVACMD=%JAVA_HOME%\bin\java.exe"
)

if "%JAVACMD%"=="" (
    if exist "C:\Users\18043\.workbuddy\binaries\java\versions\3.13.12\bin\java.exe" (
        set "JAVACMD=C:\Users\18043\.workbuddy\binaries\java\versions\3.13.12\bin\java.exe"
    ) else (
        echo ERROR: JAVA_HOME is not set and managed JDK not found
        exit /b 1
    )
)

if not exist "%JAVACMD%" (
    echo ERROR: java.exe not found at %JAVACMD%
    exit /b 1
)

cd /d "%~dp0"

"%JAVACMD%" ^
  %MAVEN_OPTS% ^
  %MAVEN_DEBUG_OPTS% ^
  -classpath "%MVN_HOME%\boot\plexus-classworlds-2.8.0.jar" ^
  "-Dclassworlds.conf=%MVN_HOME%\bin\m2.conf" ^
  "-Dmaven.home=%MVN_HOME%" ^
  "-Dmaven.multiModuleProjectDirectory=%~dp0" ^
  org.codehaus.plexus.classworlds.launcher.Launcher ^
  %MAVEN_ARGS% ^
  %*

@echo off
setlocal
set MAVEN_PROJECTBASEDIR=%~dp0

for /f "tokens=2 delims==" %%a in ('findstr "distributionUrl" "%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.properties"') do set DISTRIBUTION_URL=%%a

set MAVEN_ZIP=%TEMP%\maven-wrapper.zip
set MAVEN_HOME=%USERPROFILE%\.m2\wrapper\dists\apache-maven

if not exist "%MAVEN_HOME%\bin\mvn.cmd" (
    echo Downloading Maven...
    powershell -Command "Invoke-WebRequest -Uri '%DISTRIBUTION_URL%' -OutFile '%MAVEN_ZIP%'"
    powershell -Command "Expand-Archive -Path '%MAVEN_ZIP%' -DestinationPath '%MAVEN_HOME%' -Force"
    del "%MAVEN_ZIP%"
)

for /d %%d in ("%MAVEN_HOME%\apache-maven-*") do set MVN_CMD=%%d\bin\mvn.cmd

"%MVN_CMD%" %*
endlocal

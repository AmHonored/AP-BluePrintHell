@echo off
echo Building Network Game project...

:: Check if Maven is in PATH
where mvn >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo Maven not found in PATH. Please make sure Maven is installed and added to your PATH.
    echo You can download Maven from https://maven.apache.org/download.cgi
    pause
    exit /b 1
)

:: Build the project using Maven
mvn clean package

if %ERRORLEVEL% neq 0 (
    echo Build failed. See above for details.
    pause
    exit /b %ERRORLEVEL%
) else (
    echo Build completed successfully.
    echo You can find the built JAR file in the target directory.
    pause
) 
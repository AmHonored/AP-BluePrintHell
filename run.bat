@echo off
echo Building and running Network Game...

:: Check if Maven is in PATH
where mvn >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo Maven not found in PATH. Please make sure Maven is installed and added to your PATH.
    echo You can download Maven from https://maven.apache.org/download.cgi
    pause
    exit /b 1
)

:: Build and run the project using JavaFX Maven plugin
mvn clean javafx:run

if %ERRORLEVEL% neq 0 (
    echo Application failed to start. See above for details.
    pause
    exit /b %ERRORLEVEL%
) 
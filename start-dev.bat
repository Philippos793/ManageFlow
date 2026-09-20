@echo off
setlocal

if /I "%~1"=="backend" goto backend
if /I "%~1"=="frontend" goto frontend

if not exist "%~dp0dev.env.bat" (
    echo [ERROR] dev.env.bat was not found in the project root.
    echo Copy dev.env.bat.example to dev.env.bat and add your local values.
    pause
    exit /b 1
)

call "%~dp0dev.env.bat"
if errorlevel 1 (
    echo [ERROR] Failed to load dev.env.bat.
    pause
    exit /b 1
)

start "Employee Management Backend" cmd /k call "%~f0" backend
start "Employee Management Frontend" cmd /k call "%~f0" frontend
exit /b 0

:backend
cd /d "%~dp0employee-management-backend"

if not defined DB_PASSWORD (
    echo [ERROR] DB_PASSWORD environment variable is not defined.
    echo Set DB_PASSWORD in dev.env.bat before starting the backend.
    exit /b 1
)

if not defined JWT_SECRET (
    echo [ERROR] JWT_SECRET environment variable is not defined.
    echo Set JWT_SECRET in dev.env.bat before starting the backend.
    exit /b 1
)

set "SPRING_PROFILES_ACTIVE=dev"
echo Starting backend with the dev profile...
call mvnw.cmd spring-boot:run
exit /b %ERRORLEVEL%

:frontend
cd /d "%~dp0employee-management-frontend"
echo Starting frontend development server...
npm run dev
exit /b %ERRORLEVEL%

@echo off
title LifeLedger - MySQL Database Setup
echo ============================================================
echo  LifeLedger - MySQL Database Provisioning
echo ============================================================
echo.
echo This script will create the 'lifeledger' database and all tables.
echo Make sure your local MySQL service is running.
echo.
set /p MYSQL_USER="Enter MySQL username [default: root]: "
if "%MYSQL_USER%"=="" set MYSQL_USER=root

echo.
echo Importing schema into MySQL (enter password when prompted)...
mysql -u %MYSQL_USER% -p < src\main\resources\schema.sql

if %ERRORLEVEL% equ 0 (
    echo.
    echo ============================================================
    echo [SUCCESS] Database 'lifeledger' and tables created successfully!
    echo ============================================================
) else (
    echo.
    echo [ERROR] Failed to run schema.sql. Ensure MySQL is running
    echo and the entered password was correct.
)
pause

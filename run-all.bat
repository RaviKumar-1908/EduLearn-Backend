@echo off
setlocal enabledelayedexpansion
title LMS Control Panel
color 0B

:: ============================================================
:: SERVICES DEFINITION
:: ============================================================
set "INFRA=mysql redis rabbitmq eureka-server"
set "SERVICES=api-gateway auth-service course-service lesson-service assessment-service enrollment-service payment-service notification-service progress-service discussion-service admin-server"

:: ============================================================
:: MAIN MENU
:: ============================================================
:MENU
cls
echo.
echo  +-------------------------------------------------------+
echo  ^|             LMS - Docker Control Panel                ^|
echo  +-------------------------------------------------------+
echo.
echo   [1]  START   - Run app (no code changes)
echo   [2]  BUILD   - Rebuild + Start (after code changes)
echo   [3]  STOP    - Stop everything
echo   [4]  RESTART - Restart a single service
echo   [5]  LOGS    - View logs for a service
echo   [0]  EXIT
echo.
set /p choice="  Choose: "

if "%choice%"=="1" goto START_FAST
if "%choice%"=="2" goto BUILD_AND_START
if "%choice%"=="3" goto STOP_ALL
if "%choice%"=="4" goto RESTART_ONE
if "%choice%"=="5" goto VIEW_LOGS
if "%choice%"=="0" goto EXIT

echo.
echo  [!] Invalid choice.
timeout /t 2 >nul
goto MENU

:: ============================================================
:: [1] START FAST - No build, just run existing images
:: ============================================================
:START_FAST
cls
echo.
echo  --- Starting LMS (Existing Images) ---
echo.
echo  [1/2] Starting infrastructure...
docker compose up -d %INFRA%
echo  [*] Waiting 12 seconds for infrastructure...
timeout /t 12 >nul
echo.
echo  [2/2] Starting all services...
docker compose up -d
echo.
echo  [V] LMS is running.
echo.
pause
goto MENU

:: ============================================================
:: [2] BUILD AND START - Rebuild a service then start all
:: ============================================================
:BUILD_AND_START
cls
echo.
echo  --- Service Rebuild ---
echo.
echo  Which service did you change?
echo.
call :PRINT_SERVICE_LIST
echo.
set /p svc_num="  Service number (or 0 to rebuild ALL): "

if "%svc_num%"=="0" goto BUILD_ALL

call :RESOLVE_SERVICE "%svc_num%"
if "!svc_name!"=="" (
    echo.
    echo  [!] Invalid selection.
    timeout /t 2 >nul
    goto BUILD_AND_START
)

echo.
echo  [1/3] Running Maven build for !svc_name!...
pushd !svc_name!
call mvn clean package -DskipTests
popd
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo  [X] Maven build failed.
    pause
    goto MENU
)

echo.
echo  [2/3] Removing old container...
docker compose stop !svc_name!
docker compose rm -f !svc_name!

echo.
echo  [3/3] Starting rebuilt container...
docker compose up -d --build --force-recreate --no-deps !svc_name!

echo.
echo  [V] !svc_name! rebuilt and restarted.
echo.
pause
goto MENU

:BUILD_ALL
echo.
echo  [1/3] Running Maven build for ALL services...
call mvn clean package -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo  [X] Maven build failed.
    pause
    goto MENU
)
echo.
echo  [2/3] Starting infrastructure...
docker compose up -d %INFRA%
echo  [*] Waiting 12 seconds...
timeout /t 12 >nul
echo.
echo  [3/3] Rebuilding and starting all services...
docker compose up -d --build --force-recreate
echo.
echo  [V] Full rebuild complete.
echo.
pause
goto MENU

:: ============================================================
:: [3] STOP ALL
:: ============================================================
:STOP_ALL
cls
echo.
echo  --- Stopping All Containers ---
echo.
docker compose down --remove-orphans
echo.
echo  [V] Everything stopped.
echo.
pause
goto MENU

:: ============================================================
:: [4] RESTART ONE SERVICE
:: ============================================================
:RESTART_ONE
cls
echo.
echo  --- Single Service Restart ---
echo.
call :PRINT_SERVICE_LIST
echo.
set /p svc_num="  Service number: "

call :RESOLVE_SERVICE "%svc_num%"
if "!svc_name!"=="" (
    echo.
    echo  [!] Invalid selection.
    timeout /t 2 >nul
    goto RESTART_ONE
)

echo.
echo  [*] Restarting !svc_name!...
docker compose restart !svc_name!
echo.
echo  [V] !svc_name! restarted.
echo.
pause
goto MENU

:: ============================================================
:: [5] VIEW LOGS
:: ============================================================
:VIEW_LOGS
cls
echo.
echo  --- View Logs ---
echo.
call :PRINT_SERVICE_LIST
echo   [99] ALL services
echo.
set /p svc_num="  Service number: "

if "%svc_num%"=="99" (
    for %%s in (%INFRA% %SERVICES%) do (
        start "LOGS: %%s" cmd /k "docker compose logs -f %%s"
    )
    echo.
    echo  [V] Log windows opened.
    pause
    goto MENU
)

call :RESOLVE_SERVICE "%svc_num%"
if "!svc_name!"=="" (
    echo.
    echo  [!] Invalid selection.
    timeout /t 2 >nul
    goto VIEW_LOGS
)

start "LOGS: !svc_name!" cmd /k "docker compose logs -f !svc_name!"
echo.
echo  [V] Log window opened for !svc_name!.
echo.
pause
goto MENU

:: ============================================================
:: HELPERS
:: ============================================================
:PRINT_SERVICE_LIST
echo   [1]  mysql           [2]  redis
echo   [3]  rabbitmq        [4]  eureka-server
echo   [5]  api-gateway     [6]  auth-service
echo   [7]  course-service  [8]  lesson-service
echo   [9]  assessment-svc  [10] enrollment-svc
echo   [11] payment-svc     [12] notification-svc
echo   [13] progress-svc    [14] discussion-svc
echo   [15] admin-server
exit /b 0

:RESOLVE_SERVICE
set "svc_name="
set "num=%~1"
if "%num%"=="1"  set "svc_name=mysql"
if "%num%"=="2"  set "svc_name=redis"
if "%num%"=="3"  set "svc_name=rabbitmq"
if "%num%"=="4"  set "svc_name=eureka-server"
if "%num%"=="5"  set "svc_name=api-gateway"
if "%num%"=="6"  set "svc_name=auth-service"
if "%num%"=="7"  set "svc_name=course-service"
if "%num%"=="8"  set "svc_name=lesson-service"
if "%num%"=="9"  set "svc_name=assessment-service"
if "%num%"=="10" set "svc_name=enrollment-service"
if "%num%"=="11" set "svc_name=payment-service"
if "%num%"=="12" set "svc_name=notification-service"
if "%num%"=="13" set "svc_name=progress-service"
if "%num%"=="14" set "svc_name=discussion-service"
if "%num%"=="15" set "svc_name=admin-server"
exit /b 0

:EXIT
cls
echo.
echo  Goodbye!
timeout /t 2 >nul
exit /b 0
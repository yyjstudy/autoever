@echo off
chcp 65001 > nul

REM AutoEver 회원관리 시스템 전체 서버 종료 스크립트 (Windows)

echo 🛑 AutoEver 회원관리 시스템 종료 중...

REM Java 프로세스 중에서 Gradle 관련 프로세스 종료
echo 📱 KakaoTalk Mock Server 종료 중...
taskkill /F /FI "WINDOWTITLE eq KakaoTalk Mock*" > nul 2>&1
for /f "tokens=2" %%i in ('tasklist /FI "IMAGENAME eq java.exe" /FI "COMMANDLINE eq *kakaotalk-mock*" /FO CSV ^| find /V "PID"') do (
    if not "%%i"=="" taskkill /F /PID %%i > nul 2>&1
)

echo 📧 SMS Mock Server 종료 중...
taskkill /F /FI "WINDOWTITLE eq SMS Mock*" > nul 2>&1
for /f "tokens=2" %%i in ('tasklist /FI "IMAGENAME eq java.exe" /FI "COMMANDLINE eq *sms-mock*" /FO CSV ^| find /V "PID"') do (
    if not "%%i"=="" taskkill /F /PID %%i > nul 2>&1
)

echo 🏠 메인 애플리케이션 종료 중...
taskkill /F /FI "WINDOWTITLE eq Main App*" > nul 2>&1
for /f "tokens=2" %%i in ('tasklist /FI "IMAGENAME eq java.exe" /FI "COMMANDLINE eq *bootRun*" /FO CSV ^| find /V "PID"') do (
    if not "%%i"=="" taskkill /F /PID %%i > nul 2>&1
)

REM Gradle 데몬 종료
echo 🔧 Gradle 데몬 종료 중...
gradlew.bat --stop > nul 2>&1

echo.
echo ✅ 모든 서버가 종료되었습니다.
echo.
pause
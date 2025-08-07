@echo off
chcp 65001 > nul

REM AutoEver 회원관리 시스템 전체 서버 시작 스크립트 (Windows)

echo 🚀 AutoEver 회원관리 시스템 시작 중...

REM logs 디렉토리 생성
if not exist logs (
    mkdir logs
)

REM KakaoTalk Mock Server 백그라운드 실행
echo 📱 KakaoTalk Mock Server 시작 ^(포트 8081^)...
start /B "KakaoTalk Mock" cmd /c "gradlew.bat :mock-servers:kakaotalk-mock:bootRun > logs\kakaotalk-mock.log 2>&1"

REM SMS Mock Server 백그라운드 실행  
echo 📧 SMS Mock Server 시작 ^(포트 8082^)...
start /B "SMS Mock" cmd /c "gradlew.bat :mock-servers:sms-mock:bootRun > logs\sms-mock.log 2>&1"

REM Mock 서버들이 시작될 때까지 대기
echo ⏳ Mock 서버 시작 대기 중... ^(5초^)
timeout /t 5 /nobreak > nul

REM 메인 애플리케이션 실행
echo 🏠 메인 애플리케이션 시작 ^(포트 8080^)...
start /B "Main App" cmd /c "gradlew.bat bootRun"

echo.
echo ✅ 모든 서버가 시작되었습니다!
echo.
echo 📋 서버 정보:
echo    - 메인 API 서버: http://localhost:8080
echo    - Swagger UI: http://localhost:8080/swagger-ui/index.html
echo    - KakaoTalk Mock: http://localhost:8081
echo    - SMS Mock: http://localhost:8082
echo.
echo 🔍 로그 파일:
echo    - KakaoTalk Mock: logs\kakaotalk-mock.log
echo    - SMS Mock: logs\sms-mock.log
echo.
echo 🛑 서버를 종료하려면 이 창을 닫거나 Ctrl+C를 눌러주세요.
echo.

REM 사용자 입력 대기 (스크립트 종료 방지)
pause > nul
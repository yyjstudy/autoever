#!/bin/bash

# AutoEver 회원관리 시스템 전체 서버 시작 스크립트

echo "🚀 AutoEver 회원관리 시스템 시작 중..."

# 백그라운드에서 KakaoTalk Mock Server 실행
echo "📱 KakaoTalk Mock Server 시작 (포트 8081)..."
./gradlew :mock-servers:kakaotalk-mock:bootRun > logs/kakaotalk-mock.log 2>&1 &
KAKAOTALK_PID=$!

# 백그라운드에서 SMS Mock Server 실행  
echo "📧 SMS Mock Server 시작 (포트 8082)..."
./gradlew :mock-servers:sms-mock:bootRun > logs/sms-mock.log 2>&1 &
SMS_PID=$!

# Mock 서버들이 시작될 때까지 대기
echo "⏳ Mock 서버 시작 대기 중... (5초)"
sleep 5

# 메인 애플리케이션 실행
echo "🏠 메인 애플리케이션 시작 (포트 8080)..."
./gradlew bootRun &
MAIN_PID=$!

echo ""
echo "✅ 모든 서버가 시작되었습니다!"
echo ""
echo "📋 서버 정보:"
echo "   - 메인 API 서버: http://localhost:8080"
echo "   - Swagger UI: http://localhost:8080/swagger-ui/index.html"  
echo "   - KakaoTalk Mock: http://localhost:8081"
echo "   - SMS Mock: http://localhost:8082"
echo ""
echo "🔍 로그 파일:"
echo "   - KakaoTalk Mock: logs/kakaotalk-mock.log"
echo "   - SMS Mock: logs/sms-mock.log"
echo ""
echo "🛑 모든 서버를 종료하려면 Ctrl+C를 눌러주세요."

# 종료 신호 처리
cleanup() {
    echo ""
    echo "🛑 서버 종료 중..."
    kill $KAKAOTALK_PID 2>/dev/null
    kill $SMS_PID 2>/dev/null  
    kill $MAIN_PID 2>/dev/null
    echo "✅ 모든 서버가 종료되었습니다."
    exit 0
}

# Ctrl+C 시그널 처리
trap cleanup SIGINT

# 메인 프로세스 대기
wait $MAIN_PID
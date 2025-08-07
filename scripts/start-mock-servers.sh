#!/bin/bash

# Mock 서버 시작 스크립트
# KakaoTalk Mock Server (포트 8081) 및 SMS Mock Server (포트 8082)를 백그라운드에서 실행합니다.

echo "🚀 Mock 서버들을 시작합니다..."

# 현재 스크립트의 디렉토리에서 프로젝트 루트로 이동
cd "$(dirname "$0")/.."

# 기존 실행 중인 프로세스 확인 및 종료
echo "📍 기존 실행 중인 Mock 서버 확인..."

# KakaoTalk Mock Server (8081 포트)
KAKAO_PID=$(lsof -t -i:8081)
if [ ! -z "$KAKAO_PID" ]; then
    echo "🔴 포트 8081에서 실행 중인 프로세스를 종료합니다 (PID: $KAKAO_PID)"
    kill -9 $KAKAO_PID
    sleep 2
fi

# SMS Mock Server (8082 포트)
SMS_PID=$(lsof -t -i:8082)
if [ ! -z "$SMS_PID" ]; then
    echo "🔴 포트 8082에서 실행 중인 프로세스를 종료합니다 (PID: $SMS_PID)"
    kill -9 $SMS_PID
    sleep 2
fi

# 로그 디렉토리 생성
mkdir -p logs

echo "🟡 KakaoTalk Mock Server 시작 중... (포트: 8081)"
./gradlew :mock-servers:kakaotalk-mock:bootRun > logs/kakaotalk-mock.log 2>&1 &
KAKAO_NEW_PID=$!

echo "🟡 SMS Mock Server 시작 중... (포트: 8082)"
./gradlew :mock-servers:sms-mock:bootRun > logs/sms-mock.log 2>&1 &
SMS_NEW_PID=$!

# 서버 시작 대기
echo "⏳ 서버 시작을 기다리는 중..."
sleep 10

# 서버 상태 확인
echo ""
echo "📊 서버 상태 확인:"

# KakaoTalk Mock Server 확인
if curl -s -u autoever:1234 http://localhost:8081/actuator/health > /dev/null 2>&1 || curl -s http://localhost:8081 > /dev/null 2>&1; then
    echo "✅ KakaoTalk Mock Server (포트 8081) - 실행 중 (PID: $KAKAO_NEW_PID)"
    echo "   - URL: http://localhost:8081/kakaotalk-messages"
    echo "   - Auth: autoever/1234"
else
    echo "❌ KakaoTalk Mock Server (포트 8081) - 시작 실패"
    echo "   - 로그 확인: tail -f logs/kakaotalk-mock.log"
fi

# SMS Mock Server 확인
if curl -s -u autoever:5678 http://localhost:8082/actuator/health > /dev/null 2>&1 || curl -s http://localhost:8082 > /dev/null 2>&1; then
    echo "✅ SMS Mock Server (포트 8082) - 실행 중 (PID: $SMS_NEW_PID)"
    echo "   - URL: http://localhost:8082/sms"
    echo "   - Auth: autoever/5678"
else
    echo "❌ SMS Mock Server (포트 8082) - 시작 실패"
    echo "   - 로그 확인: tail -f logs/sms-mock.log"
fi

echo ""
echo "📝 사용법:"
echo "  - KakaoTalk API 테스트:"
echo "    curl -X POST -u autoever:1234 -H 'Content-Type: application/json' \\"
echo "         -d '{\"phone\":\"010-1234-5678\",\"message\":\"테스트 메시지\"}' \\"
echo "         http://localhost:8081/kakaotalk-messages"
echo ""
echo "  - SMS API 테스트:"
echo "    curl -X POST -u autoever:5678 -H 'Content-Type: application/json' \\"
echo "         -d '{\"message\":\"테스트메시지\"}' \\"
echo "         'http://localhost:8082/sms?phone=010-1234-5678'"
echo ""
echo "  - 서버 종료: ./scripts/stop-mock-servers.sh"
echo "  - 로그 확인: tail -f logs/kakaotalk-mock.log logs/sms-mock.log"

# PID 파일 저장 (나중에 종료 시 사용)
echo $KAKAO_NEW_PID > logs/kakaotalk-mock.pid
echo $SMS_NEW_PID > logs/sms-mock.pid

echo ""
echo "🎉 Mock 서버 시작 완료!"
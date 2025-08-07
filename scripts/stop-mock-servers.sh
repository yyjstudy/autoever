#!/bin/bash

# Mock 서버 종료 스크립트
# 실행 중인 KakaoTalk Mock Server 및 SMS Mock Server를 종료합니다.

echo "🛑 Mock 서버들을 종료합니다..."

# 현재 스크립트의 디렉토리에서 프로젝트 루트로 이동
cd "$(dirname "$0")/.."

# PID 파일에서 프로세스 종료 시도
if [ -f logs/kakaotalk-mock.pid ]; then
    KAKAO_PID=$(cat logs/kakaotalk-mock.pid)
    if kill -0 $KAKAO_PID 2>/dev/null; then
        echo "🔴 KakaoTalk Mock Server 종료 중... (PID: $KAKAO_PID)"
        kill -15 $KAKAO_PID
        sleep 3
        # 강제 종료가 필요한 경우
        if kill -0 $KAKAO_PID 2>/dev/null; then
            kill -9 $KAKAO_PID
            echo "⚠️  KakaoTalk Mock Server 강제 종료됨"
        else
            echo "✅ KakaoTalk Mock Server 정상 종료됨"
        fi
    fi
    rm -f logs/kakaotalk-mock.pid
fi

if [ -f logs/sms-mock.pid ]; then
    SMS_PID=$(cat logs/sms-mock.pid)
    if kill -0 $SMS_PID 2>/dev/null; then
        echo "🔴 SMS Mock Server 종료 중... (PID: $SMS_PID)"
        kill -15 $SMS_PID
        sleep 3
        # 강제 종료가 필요한 경우
        if kill -0 $SMS_PID 2>/dev/null; then
            kill -9 $SMS_PID
            echo "⚠️  SMS Mock Server 강제 종료됨"
        else
            echo "✅ SMS Mock Server 정상 종료됨"
        fi
    fi
    rm -f logs/sms-mock.pid
fi

# 포트 기반으로 남은 프로세스 확인 및 종료
KAKAO_PORT_PID=$(lsof -t -i:8081)
if [ ! -z "$KAKAO_PORT_PID" ]; then
    echo "🔴 포트 8081에서 실행 중인 추가 프로세스 종료 (PID: $KAKAO_PORT_PID)"
    kill -9 $KAKAO_PORT_PID
fi

SMS_PORT_PID=$(lsof -t -i:8082)
if [ ! -z "$SMS_PORT_PID" ]; then
    echo "🔴 포트 8082에서 실행 중인 추가 프로세스 종료 (PID: $SMS_PORT_PID)"
    kill -9 $SMS_PORT_PID
fi

# 포트 상태 확인
echo ""
echo "📊 포트 상태 확인:"
PORT_8081=$(lsof -t -i:8081)
PORT_8082=$(lsof -t -i:8082)

if [ -z "$PORT_8081" ]; then
    echo "✅ 포트 8081 - 사용 가능"
else
    echo "❌ 포트 8081 - 여전히 사용 중 (PID: $PORT_8081)"
fi

if [ -z "$PORT_8082" ]; then
    echo "✅ 포트 8082 - 사용 가능"
else
    echo "❌ 포트 8082 - 여전히 사용 중 (PID: $PORT_8082)"
fi

echo ""
echo "🎉 Mock 서버 종료 완료!"
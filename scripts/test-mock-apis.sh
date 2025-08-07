#!/bin/bash

# Mock API 테스트 스크립트
# KakaoTalk 및 SMS Mock 서버의 API 동작을 테스트합니다.

echo "🧪 Mock API 테스트를 시작합니다..."

# 색상 코드
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 테스트 결과 추적
TOTAL_TESTS=0
PASSED_TESTS=0

# 테스트 함수
test_api() {
    local test_name="$1"
    local url="$2"
    local method="$3"
    local auth="$4"
    local data="$5"
    local expected_status="$6"
    local content_type="$7"
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    echo -e "\n${BLUE}🔍 테스트: $test_name${NC}"
    
    if [ "$content_type" = "json" ]; then
        response=$(curl -s -w "\n%{http_code}" -X $method -u $auth -H "Content-Type: application/json" -d "$data" $url)
    else
        response=$(curl -s -w "\n%{http_code}" -X $method -u $auth -d "$data" $url)
    fi
    
    body=$(echo "$response" | head -n -1)
    status_code=$(echo "$response" | tail -n 1)
    
    if [ "$status_code" = "$expected_status" ]; then
        echo -e "${GREEN}✅ PASS - HTTP $status_code${NC}"
        echo -e "   응답: $body"
        PASSED_TESTS=$((PASSED_TESTS + 1))
    else
        echo -e "${RED}❌ FAIL - 예상: HTTP $expected_status, 실제: HTTP $status_code${NC}"
        echo -e "   응답: $body"
    fi
}

echo -e "\n${YELLOW}📡 KakaoTalk Mock API 테스트${NC}"

# KakaoTalk API 정상 케이스
test_api "KakaoTalk 정상 메시지 발송" \
    "http://localhost:8081/kakaotalk-messages" \
    "POST" \
    "autoever:1234" \
    '{"phone":"010-1234-5678","message":"안녕하세요 테스트 메시지입니다"}' \
    "200" \
    "json"

# KakaoTalk API 에러 케이스 - 전화번호 누락
test_api "KakaoTalk 전화번호 누락 에러" \
    "http://localhost:8081/kakaotalk-messages" \
    "POST" \
    "autoever:1234" \
    '{"message":"메시지만 있는 경우"}' \
    "400" \
    "json"

# KakaoTalk API 에러 케이스 - 메시지 누락
test_api "KakaoTalk 메시지 누락 에러" \
    "http://localhost:8081/kakaotalk-messages" \
    "POST" \
    "autoever:1234" \
    '{"phone":"010-1234-5678"}' \
    "400" \
    "json"

# KakaoTalk API 서버 오류 시뮬레이션
test_api "KakaoTalk 서버 오류 시뮬레이션" \
    "http://localhost:8081/kakaotalk-messages" \
    "POST" \
    "autoever:1234" \
    '{"phone":"010-9999-5678","message":"서버 오류 테스트"}' \
    "500" \
    "json"

# KakaoTalk API 인증 오류
test_api "KakaoTalk 인증 오류" \
    "http://localhost:8081/kakaotalk-messages" \
    "POST" \
    "wrong:password" \
    '{"phone":"010-1234-5678","message":"인증 테스트"}' \
    "401" \
    "json"

echo -e "\n${YELLOW}📱 SMS Mock API 테스트${NC}"

# SMS API 정상 케이스
test_api "SMS 정상 메시지 발송" \
    "http://localhost:8082/sms?phone=010-1234-5678" \
    "POST" \
    "autoever:5678" \
    '{"message":"SMS 테스트 메시지입니다"}' \
    "200" \
    "json"

# SMS API 에러 케이스 - 전화번호 누락
test_api "SMS 전화번호 누락 에러" \
    "http://localhost:8082/sms" \
    "POST" \
    "autoever:5678" \
    '{"message":"전화번호가 없는 경우"}' \
    "400" \
    "json"

# SMS API 발송량 초과 시뮬레이션
test_api "SMS 발송량 초과 시뮬레이션" \
    "http://localhost:8082/sms?phone=010-9999-5678" \
    "POST" \
    "autoever:5678" \
    '{"message":"발송량 초과 테스트"}' \
    "429" \
    "json"

# SMS API 서버 오류 시뮬레이션
test_api "SMS 서버 오류 시뮬레이션" \
    "http://localhost:8082/sms?phone=010-8888-5678" \
    "POST" \
    "autoever:5678" \
    '{"message":"서버 오류 테스트"}' \
    "500" \
    "json"

# SMS API 잘못된 번호 시뮬레이션
test_api "SMS 잘못된 번호 시뮬레이션" \
    "http://localhost:8082/sms?phone=010-6666-5678" \
    "POST" \
    "autoever:5678" \
    '{"message":"잘못된 번호 테스트"}' \
    "400" \
    "json"

# SMS API 인증 오류
test_api "SMS 인증 오류" \
    "http://localhost:8082/sms?phone=010-1234-5678" \
    "POST" \
    "wrong:password" \
    '{"message":"인증 테스트"}' \
    "401" \
    "json"

echo -e "\n${YELLOW}📊 테스트 결과 요약${NC}"
echo -e "전체 테스트: $TOTAL_TESTS"
echo -e "${GREEN}성공: $PASSED_TESTS${NC}"
echo -e "${RED}실패: $((TOTAL_TESTS - PASSED_TESTS))${NC}"

if [ $PASSED_TESTS -eq $TOTAL_TESTS ]; then
    echo -e "\n${GREEN}🎉 모든 테스트가 통과했습니다!${NC}"
    exit 0
else
    echo -e "\n${RED}⚠️  일부 테스트가 실패했습니다.${NC}"
    echo -e "${YELLOW}서버가 실행 중인지 확인해주세요:${NC}"
    echo -e "  - KakaoTalk Mock: http://localhost:8081"
    echo -e "  - SMS Mock: http://localhost:8082"
    exit 1
fi
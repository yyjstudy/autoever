Prerequisites
본인의 github 저장소(public)를 하나 준비해주세요.
Spring boot framework 기반 환경에서 서버를 구현하세요.
언어는 Java, Kotlin 사용이 가능합니다.
본인이 구현한 서버를 면접관이 로컬 환경에서 실행해 볼 수 있도록 README.md 를 작성하세요.
면접관은 추후에 제출하신 본인의 github 저장소를 clone 받아서 서버를 실행해 볼 예정입니다.
저장소(DB, Redis 등)는 자유롭게 사용 가능 가능합니다.
단, 면접관이 local 환경에서 해당 저장소를 실행해 볼 수 있도록 README.md 파일에 가이드를 작성해 주세요.
예) 서버 실행 전, 터미널에서 아래와 같이 실행하시오.
$> docker pull redis:latest && docker run -d -p 6379:6379 redis:latest
과제 수행 간, AI assistence 를 최대한 적극적으로 활용해주세요.
단, PROMPT.md 파일을 별도로 생성하셔서, 활용하신 모든 프롬프트들을 기록해 주세요.

Requirements
주어진 요구사항들을 순서에 맞게 구현해 주세요.
단, 하나의 요구사항을 모두 구현 완료한 후에 다음 요구사항을 구현해야 합니다.
회원가입 API 가 필요합니다.
사용자 API) 간단한 회원가입 API 를 구현해 주세요.
사용자 입력 값은 계정/암호/성명/주민등록번호/핸드폰번호/주소 입니다.
핸드폰번호, 주민등록번호는 11자리 등의 자릿수... 구색만 맞추는 것이지, 실제 본인이나 제3자의 주민등록번호, 핸드폰번호를 사용할 필요는
없습니다.
즉, 과제를 위해 필요한 것이지, 본인인증 등을 고려할 필요는 없습니다.
서버는 사용자의 요청을 그냥 믿고 회원가입 성공 처리를 하시면 됩니다.
시스템 내에서 계정값과 주민등록번호 값은 유일해야 합니다.
시스템 관리자가 사용할 API 가 필요합니다.
시스템 관리자 API) 회원 조회, 수정, 삭제 API 를 구현해 주세요.
조회는 pagination 기반으로 가능해야 합니다.
수정은 암호, 주소에 대해서만 가능합니다. 한가지씩 수정도 가능하고, 두가지를 동시에 수정도 가능합니다.
시스템 관리자 API 들의 인증 수단은 basic auth 기반이어야 합니다.
사용자명: admin
암호: 1212
회원 가입한 사용자들이 로그인 할 수 있어야 합니다.
사용자 API) 로그인 API 를 구현해주세요.
로그인 한 사용자는 자신의 회원 상세정보를 조회할 수 있어야 합니다.
사용자 API) 본인의 상세정보를 내려받을 수 있는 API 를 구현해주세요.
단, 이 API 에서는 주소 값을 모두 내려주지는 않습니다. 가장 큰 단위의 행정구역 단어만을 제공해야 합니다. 예) "서울특별시" or "경기도" or "강원특별자치도" ...
본 서비스가 이른바 "대박"이 났습니다. 사용자수가 3천만을 돌파했습니다.
아래와 같은 요구사항이 추가로 주어졌습니다.
관리자 API) 모든 회원을 대상으로, 연령대별 카카오톡 메세지를 발송할 수 있는 API를 만들어 주시오.
메세지의 첫줄은 항상 "{회원 성명}님, 안녕하세요. 현대 오토에버입니다." 이어야 합니다.
카카오톡 메세지를 보내는 데 실패할 경우 SMS 문자메세지를 보내야 합니다.
카카오톡 메세지는 카카오 정책에 따라, 발급된 토큰 당 1분당 100회까지만 호출이 가능합니다.
문자 메세지는 써드파티 벤더사 정책에 따라, 분당 500회 제한이 존재합니다.
카카오톡 메세지 발송을 위한 API 명세는 아래와 같습니다.
POST http://localhost:8081/kakaotalk-messages
헤더
Authorization (Basic auth)
사용자명: autoever
암호: 1234
content-type (applciation/json)
요청바디
{"phone": "xxx-xxxx-xxxx", "message": "blabla"}
서버 response http status code: 200 or 400 or 401 or 500
응답 바디: 없음
문자메세지 발송을 위한 API 명세는 아래와 같습니다.
POST http://localhost:8082/sms?phone={phone}
헤더
Authorization (Basic auth)
사용자명: autoever
암호: 5678
content-type ( )
application/x-www-form-urlencoded
{"message": "blabla"}
서버 response http status code: 200 or 400 or 401 or 500
응답 바디: application/json {"result": "OK"}

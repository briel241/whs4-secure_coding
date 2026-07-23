# 아키텍처

## 구성

브라우저 → Spring Security 필터 → Spring MVC Controller → 기능 Service → Spring Data JPA Repository → PostgreSQL 순서의 서버 렌더링 모놀리스다. Controller는 DTO만 바인딩하고 Service가 권한과 트랜잭션을 책임진다.

## 기능 패키지

- `auth`: 가입 DTO, 정규화, 사용자 상세, 로그인 성공/실패 처리
- `user`: 사용자 Entity/Repository, 프로필/탈퇴/세션 무효화
- `product`: 카테고리, 상품, 검색, 이미지 저장/조회
- `chat`: 대화, 메시지, 읽음, 전송 제한
- `wallet`: 지갑, intent, 송금, 원장, 조정
- `moderation`: 신고와 관리 조치
- `admin`: 감사 로그와 관리자 화면/서비스
- `common`: 공통 Entity 시간, 업무/404 예외, 일반화 오류 화면
- `config`: 보안, 속성, demo/관리자 부트스트랩

## 권한 경계

- 공개: 홈, 가입/로그인, 공개 상품/이미지
- 인증: 프로필, 상품 쓰기, 대화, 신고, 지갑
- ADMIN: `/admin/**`
- 관계 검증: 상품 `seller_id`, 대화 buyer/seller, 메시지 conversation 참여자, 지갑 user, intent sender를 Repository 조건 또는 Service에서 다시 확인한다.

UUID는 추측 난이도만 높일 뿐 권한으로 취급하지 않는다. 비공개 자원은 가능한 경우 404로 존재를 숨긴다.

## 송금 순서

1. 준비: 사용자·지갑·잔액·한도를 검증하고 서버 DB에 3분 intent와 idempotency key 저장
2. 확인: DB의 수신자/금액 표시, 현재 비밀번호와 CSRF 검증
3. 실행: intent 잠금 → 두 지갑 UUID 순서 잠금 → 재검증 → 잔액 → 송금 → 음/양 원장 → intent USED
4. 실패: 같은 PostgreSQL 트랜잭션 전체 rollback

## 이미지 경계

원본 이름은 경로에 쓰지 않는다. 형식/서명/디코딩/크기/픽셀을 검증한 뒤 PNG로 재인코딩하고, 실행 경로 밖의 `UPLOAD_DIR`에 UUID 키로 저장한다. 조회 Handler만 바이트를 제공하며 `Content-Type`과 `nosniff`를 설정한다.

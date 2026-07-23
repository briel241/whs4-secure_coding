# 테스트

## 환경

- 로컬: Windows 11, JDK 23.0.1의 `--release 21`
- CI: Temurin 21
- Gradle 8.14.4
- Testcontainers 1.21.4
- PostgreSQL 17.10 Alpine
- JUnit 5, Spring Boot Test, MockMvc

## 실행 결과

2026-07-24 최종 실행:

```text
30 tests completed, 30 passed
BUILD SUCCESSFUL
```

실제 Testcontainers PostgreSQL에서 Flyway와 `ddl-auto=validate`를 적용했다. H2는 사용하지 않았다.

동일한 PostgreSQL 17.10 Docker Compose 환경에서 생성된 실행 JAR을 시작해 Flyway V1 적용, JPA 스키마 검증, Tomcat 8080 기동까지 확인했다. 연결 가능한 인앱 브라우저 세션이 없어 화면별 수동 조작 검증은 수행하지 못했다.

## 체크리스트

- 인증: 가입, 이메일·닉네임 정규화 중복, 가입·프로필 닉네임 중복 필드 강조와 변경 원복, salted Argon2, mass assignment, 잘못된 로그인 반복 잠금, 정지 계정, CSRF 로그아웃
- 상품: 등록, 음수 가격, 쉼표가 포함된 원화 가격 등록·검색·표시, 타인 변경/삭제 IDOR, 삭제 비노출, 검색어/정렬/페이지 제한, XSS 이스케이프
- 파일: PNG 정상, 위장 MIME/내용, 이중 확장자, 경로 순회, SVG, 손상, 5MB 초과, 픽셀 폭 초과
- 대화·신고: 단일 조합, 자기 상품 거부, 비참여자 거부, 일반 텍스트 XSS, 읽지 않음, 429, 상대 사용자·수신 메시지 신고 링크, 자기/무관 메시지 신고 거부
- 관리자: 일반 사용자 ADMIN 경로 거부, 전체 관리 메뉴 노출, 상품 숨김 비노출/감사, 사유 필수, 마지막 관리자, 사용자 정지/지갑 동결
- 송금: 정상, 다른 사용자 intent, 재인증 실패, 순차/병렬 idempotency, 100에서 80 두 건 경쟁, 반대 방향 교착 방지, 원장 합계 0, DB 중간 실패 rollback, 잔액 대조

## 아직 자동화가 약한 항목

- JPEG/WebP 정상 업로드 각각의 고정 fixture
- 브라우저별 반응형/키보드 수동 접근성
- intent 3분 만료와 일일 5,000,000 한도 전용 경계 테스트
- 관리자 메시지 숨김/복원과 신고자 비노출의 HTML 응답 회귀 테스트
- 상품 낙관적 잠금 충돌의 병렬 테스트

해당 로직/제약은 구현되어 있으나 위 항목은 향후 테스트 보강 우선순위다.

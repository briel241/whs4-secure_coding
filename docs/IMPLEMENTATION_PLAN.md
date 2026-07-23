# Tiny Second-hand Shopping Platform 구현 계획

## 1. 조사 결과

- 조사일: 2026-07-23
- 작업 폴더는 비어 있었고 Git 저장소가 아니었다.
- 루트 `AGENTS.md`와 `outputs/PROJECT_BLUEPRINT_KO.md`는 존재하지 않았다.
- 따라서 첨부된 과제 요구사항을 기준 문서로 사용하며, 누락된 문서의 내용을 추측하지 않는다.
- 로컬 Java 런타임은 23.0.1이며 Gradle은 설치되어 있지 않다. Gradle Wrapper와 Java 21 toolchain/release 설정으로 재현성을 확보한다.
- Docker CLI는 설치되어 있으나 실제 Docker daemon과 Testcontainers 사용 가능 여부는 M0 검증에서 확인한다.

## 2. 고정 기술 버전

- Java toolchain/release: 21
- Spring Boot: 3.5.16
- Gradle Wrapper: 8.14.4
- PostgreSQL: 17.10
- Testcontainers: Spring Boot 3.5.16 의존성 관리 버전

Spring Boot 3.5는 Java 21을 공식 지원하는 안정 유지보수 계열이며, Spring Security 6 기반 서버 세션 인증과 Spring Data JPA 요구사항에 적합하다. PostgreSQL은 현재 지원되는 최신 안정 마이너 버전을 사용한다.

## 3. 보안 불변조건

1. 인증은 서버 세션만 사용하고 CSRF를 활성화한다.
2. 공개 경로만 허용하고 나머지는 인증 또는 `ADMIN` 역할을 요구한다.
3. Controller는 요청 DTO만 바인딩하고 Entity를 직접 바인딩하지 않는다.
4. 상품·대화·메시지·지갑·송금 intent의 소유/참여 관계를 Service와 Repository 조건에서 확인한다.
5. 비밀번호는 Argon2id로 해시하고 평문·해시·세션·CSRF·쿠키·메시지 본문을 로그에 남기지 않는다.
6. 상품 설명과 메시지는 일반 텍스트로 저장하며 Thymeleaf 이스케이프 출력만 사용한다.
7. 업로드 이미지는 확장자, MIME, 시그니처, 디코딩, 픽셀, 크기, 개수를 모두 검증한 뒤 UUID 키로 외부 저장소에 재인코딩한다.
8. 송금 값은 서버 저장 intent만 신뢰하고, 재인증·CSRF·소유권·만료·idempotency를 확인한다.
9. 송금은 PostgreSQL 행 잠금과 단일 트랜잭션으로 처리하고 복식 원장 합계를 0으로 유지한다.
10. 완료 송금, 원장, 감사 로그는 애플리케이션에서 수정·삭제 경로를 제공하지 않는다.

## 4. 마일스톤

### M0 기반

- Gradle/Spring Boot 프로젝트, PostgreSQL Compose, Flyway V1
- 환경 설정, 공통 예외/오류 페이지, 보안 헤더 기본값
- 애플리케이션 컨텍스트 및 Flyway 기본 테스트

### M1 인증

- 사용자, 역할/상태, 지갑 생성
- 가입, 로그인, 로그아웃, 프로필, 탈퇴
- Argon2id, 정규화 중복, 로그인 실패 제한, 세션 무효화
- 정상·실패·mass assignment·CSRF 테스트

### M2 상품

- 카테고리, 상품 CRUD/상태/소프트 삭제
- 검색·필터·정렬 allowlist·페이지 크기 제한
- 안전한 이미지 저장/조회
- 소유권·비노출·XSS·SQL 형태 검색·파일 우회 테스트

### M3 대화

- 상품별 구매자/판매자 단일 대화, 메시지, 읽음 상태
- 참여자 조건 Repository 조회, 메시지 rate limit
- 중복·비참여자·XSS·길이·429 테스트

### M4 신고와 관리자

- 신고 접수/중복 제한, 사용자·상품·메시지 관리
- 카테고리 관리, 조사 감사 로그, 마지막 관리자 보호
- 일반 사용자 접근 거부·사유 필수·비노출·감사 테스트

### M5 지갑과 송금

- 3분 intent, 비밀번호 재인증, idempotency
- 결정적 순서의 `SELECT FOR UPDATE`, 복식 원장, 일일 한도
- Testcontainers PostgreSQL 순차/병렬 중복, 잔액 경쟁, rollback, 교차 송금 테스트

### M6 통합 보안

- CSRF/XSS/SQL Injection/IDOR/업로드/송금 회귀 테스트
- 의존성·정적 분석·비밀값 검사 설정 및 가능한 로컬 검사

### M7 제출 문서

- README와 요구사항/아키텍처/DB/테스트/보안/유지보수/최종 보고서
- GitHub Actions, Dependabot, CodeQL, 템플릿, SECURITY, LICENSE

## 5. 검증 전략

- 단위 테스트: 정규화, 검증기, rate limit, 상태 전이, 원장 대조
- MVC 통합 테스트: 인증/CSRF/역할/소유권/화면 렌더링
- JPA 통합 테스트: PostgreSQL 제약, 조건 조회, 낙관적 잠금
- Testcontainers 동시성 테스트: 행 잠금, idempotency, rollback, 반대 방향 송금
- 수동 검증: Docker Compose로 깨끗한 DB 마이그레이션, demo 프로필 화면 흐름

## 6. 완료 판정

각 요구사항은 `docs/requirements.md`에서 코드와 테스트에 연결한다. 실행하지 못한 검사는 통과로 기록하지 않으며, Critical/High 보안 불변조건 위반이나 테스트 실패가 남으면 완료로 표시하지 않는다.

## 7. 구현 결과

M0~M7 코드와 문서를 완료했다. 2026-07-24 Testcontainers PostgreSQL 17.10에서 30개 테스트를 실행해 모두 통과했다. GitHub 공개는 사용자 승인을 받아 진행하며, 배포·브라우저 수동 QA·외부 취약점 서비스 결과는 자동 테스트 완료와 구분해 `STATUS.md`에 남겼다.

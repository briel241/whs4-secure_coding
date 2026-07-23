# Tiny Second-hand Shopping Platform 개발 보고서

## 1. 서론

이 프로젝트는 단순 화면 목업이 아니라 PostgreSQL에서 실행되고 자동 테스트되는 서버 렌더링 중고거래 플랫폼을 목표로 했다. 핵심 관점은 기능 수보다 인증, 자원 관계 권한, 안전한 업로드, 관리자 감사, 가상 포인트 원장 무결성을 실제 서버와 DB에서 강제하는 것이다.

## 2. 요구사항 분석

과제의 일곱 요구사항을 가입, 상품 등록·조회, 사용자 소통, 악성 사용자·상품 차단, 사용자 간 송금, 상품 검색, 관리자 전체 관리로 분해했다. 이미지·신고·감사·문서·CI는 이 요구사항을 안전하게 완성하기 위한 하위 기능과 품질 항목으로 추적했다. 실제 금융정보를 수집하지 않고 포인트를 BIGINT 정수로만 취급했다. 별도 SPA, JWT/localStorage, WebSocket, Redis, Elasticsearch, H2는 사용하지 않았다.

상세 추적은 [requirements.md](requirements.md)에 코드와 테스트까지 연결했다.

## 3. 역할과 권한

- 비회원: 홈, 공개 상품 목록/상세/이미지, 가입/로그인
- USER: 프로필, 상품 쓰기, 대화, 신고, 본인 지갑/송금
- ADMIN: 사용자/상품/메시지/신고/카테고리/송금/감사 조회와 관리

URL 역할 검사만 믿지 않고 Service와 Repository에서 seller, buyer/seller participant, wallet user, intent sender 관계를 확인한다. 비공개 자원은 404로 숨긴다.

## 4. 시스템 설계

Spring MVC/Thymeleaf 서버 렌더링 모놀리스와 Spring Security 서버 세션을 채택했다. Controller는 HTTP와 DTO, Service는 업무 규칙/권한/트랜잭션, Repository는 조건 조회, Entity는 영속성을 담당한다. 기능 중심 패키지를 사용해 인증/사용자/상품/대화/지갑/관리 영역의 변경 경계를 분명히 했다.

## 5. DB 설계

Flyway V1에 13개 주요 테이블과 FK/UNIQUE/CHECK/인덱스를 정의했다. 상품에는 낙관적 잠금 version, 지갑에는 version과 음수 방지, 대화에는 product+buyer+seller unique, 송금에는 intent와 sender+idempotency unique를 둔다. 정상 송금은 합이 0인 두 원장을 생성하며 demo grant도 원장에 기록한다.

상세는 [database.md](database.md)에 있다.

## 6. 기능 구현

### 인증

이메일/닉네임 NFKC 정규화, 정규화 닉네임 전역 UNIQUE, 가입·프로필 중복 필드 오류, Argon2id, 12~128자 비밀번호, 가입+지갑 단일 트랜잭션, 실패 잠금, 정지 계정 로그인 거부, 세션 만료, POST/CSRF 로그아웃을 구현했다. 송금 수신자는 유일한 정규화 닉네임으로 한 명만 결정한다.

### 상품과 이미지

상품 CRUD/상태/소프트 삭제/카테고리/검색/필터/정렬/페이지를 제공한다. 이미지는 이름, 확장자, MIME, 시그니처, 디코딩, 크기, 픽셀, 개수를 검증하고 PNG로 재인코딩한다.

### 대화

상품/구매자/판매자 조합을 unique로 유지한다. 참여자 조건 조회, 일반 텍스트, 읽음 시각/미확인 수, 30초 10건 제한을 적용했다.

### 신고와 관리자

상품 상세와 대화 화면에서 사용자/상품/메시지 신고를 제공하고, 24시간 같은 대상 중복 제한과 자기·무관 메시지 신고 거부를 적용했다. 관리자는 대시보드에서 사용자 상태, 상품·메시지 공개 상태, 신고, 카테고리, 송금, 감사를 관리한다. 대화 본문은 POST+CSRF와 조사 사유를 거쳐 열람하며 모든 조치는 actor, 대상, 전후 상태, 사유, 결과, 시간을 기록한다.

### 가상 포인트

3분 intent와 서버 보관 idempotency key, 비밀번호 재확인, CSRF, 소유권, 한도, 결정적 지갑 행 잠금, 단일 트랜잭션 복식 원장, 잔액 대조를 구현했다.

## 7. 보안 위협 분석

주요 위협은 CSRF 상태 변경, Stored XSS, SQL Injection, UUID IDOR, DTO mass assignment, 세션 fixation, brute force/사용자 열거, 업로드 polyglot/경로 순회/이미지 폭탄, 메시지 남용, 관리자 오용, 송금 중복/경쟁/rollback/교착이다. 각 위협을 애플리케이션 방어와 DB 제약, 회귀 테스트 중 둘 이상으로 다뤘다.

## 8. 발견한 보안 약점

발견하지 않은 취약점을 창작하지 않았다.

- 구현 리뷰: 외부 폰트 요청이 CSP/개인정보 최소화 방향과 맞지 않아 제거했다.
- 구현 리뷰: 업로드 정리 실패가 조용히 무시되어 저장 키만 남기는 안전한 경고 로그로 바꿨다.
- 테스트: 이메일 trim이 Bean Validation보다 늦어 요구한 정규화 가입이 실패함을 발견했다.
- 테스트 인프라: 컨테이너 종료 뒤 캐시 컨텍스트가 이전 DB 주소를 쓰는 문제를 발견했다.
- 도구: Windows wrapper가 실패 종료 코드를 숨기는 문제를 발견했다.
- 기능 추적: 사용자·메시지 신고 서버 로직은 있었지만 실제 화면 진입점이 없어 상품·대화 화면에 신고 버튼을 추가했다.
- 관리자 흐름: 메시지 조사 기능이 대시보드에 노출되지 않았고 GET 요청이 감사 기록을 생성해, 관리 메뉴·신고 바로가기를 추가하고 조사 실행을 POST+CSRF로 변경했다.
- 관리자 목록: 서버는 페이지 번호를 받았지만 화면에 이전/다음 링크가 없어, 사용자·상품·신고·송금·감사 목록 페이지 이동과 신고 대상 상품 전용 관리 화면을 추가했다.

## 9. 수정 전후 비교

| 항목 | 수정 전 | 수정 후 |
|---|---|---|
| 이메일 공백 | `@Email` 단계에서 거부 | 이메일 필드만 검증 전 trim |
| Testcontainers | 첫 클래스 뒤 DB 종료 | JVM 전체에서 단일 PostgreSQL 유지 |
| Gradle 실패 | batch가 0을 반환할 가능성 | 원래 errorlevel 반환 |
| CSS 글꼴 | 외부 Google 요청 | OFL Pretendard 로컬 자체 호스팅 |
| 업로드 정리 | 실패 무시 | 민감값 없는 WARN |
| 닉네임 중복 피드백 | 가입 폼 전체 오류, 커밋 시점 제약 오류 가능 | 가입·프로필 필드 오류와 즉시 DB flush |
| 사용자·메시지 신고 | 서버 엔드포인트만 존재 | 상품·대화 화면의 명시적 신고 버튼과 자기/무관 메시지 신고 거부 |
| 관리자 메시지 조사 | 숨은 UUID 진입, GET이 감사 기록 생성 | 대시보드·신고 상세 진입, POST+CSRF 조사와 감사 기록 |
| 관리자 전체 목록 | 서버 페이지 파라미터만 존재 | 목록별 이전/다음 UI와 신고 대상 상품 직접 관리 |

## 10. 테스트 환경

Windows 11, JDK 23.0.1 `--release 21`, Gradle 8.14.4, Testcontainers 1.21.4, PostgreSQL 17.10, Spring Boot Test/MockMvc/JUnit 5를 사용했다. CI는 Temurin 21을 고정한다.

## 11. 테스트 체크리스트

최종 실행에서 30개 테스트가 모두 통과했다. 가입/로그인/CSRF/mass assignment, 가입·프로필 정규화 닉네임 중복과 필드 강조, 상품 IDOR/XSS/검색 경계와 쉼표 원화 입력, 이미지 정상/위장/경로/크기/픽셀, 대화 단일성/참여자/읽음/429/신고 링크와 신고 대상 권한, 관리자 기본 거부/전체 관리 메뉴/감사/마지막 관리자, 송금 정상/intent 소유/재인증/중복/잔액 경쟁/반대 방향/rollback/원장 대조를 포함한다.

상세와 미자동화 항목은 [testing.md](testing.md)에 구분했다.

## 12. 실패와 수정 사례

1. 로컬 JDK 21 부재: JDK 23의 `--release 21`을 사용하고 CI 실제 JDK 21 검증을 추가했다.
2. OneDrive가 `build/classes`를 잠금: 로컬에서만 `TINYSHOP_BUILD_DIR`로 임시 폴더를 지정할 수 있게 했다.
3. 첫 PostgreSQL 테스트 14건 실패: 제품 로직을 추측 수정하지 않고 테스트 XML을 확인해 컨테이너 수명주기를 수정했다.
4. 이메일 공백 가입 테스트 실패: 비밀번호를 건드리지 않고 이메일 프로퍼티만 trim했다.
5. 송금 rollback 확인: DB trigger로 원장 insert 중 예외를 발생시켜 잔액/송금이 전부 원복되는지 검증했다.
6. PostgreSQL 18.4 Compose를 처음 시도하며 바뀐 volume 규칙과 Flyway 공식 지원 범위 경고를 확인했다. 안정성을 위해 Flyway 시험 범위인 PostgreSQL 17.10과 major-version 전용 volume으로 고정했다. 실패한 초기 volume은 자동 삭제하지 않았다.

## 13. GitHub와 CI

Temurin 21 테스트/bootJar, Wrapper validation, 실패 보고서 업로드, dependency submission, CodeQL 주간/PR 분석, Dependabot Gradle/Actions 설정을 추가했다. PR/Issue 템플릿, SECURITY, MIT License, `.gitignore`, `.env.example`도 준비했다.

Git 초기화, commit, push와 공개는 사용자 승인을 받은 뒤 `briel241/whs4-secure_coding`을 대상으로 진행했다. GitHub Actions의 원격 CI/CodeQL 결과는 로컬 검증 결과와 구분해 확인한다.

## 14. 유지보수

의존성/CodeQL 주간 검토, 월간 깨끗한 DB 테스트, Flyway append-only, 원장/감사 대조, 업로드 용량/고아 파일 점검, 백업 복구 훈련을 권장한다. 상세 절차는 [maintenance.md](maintenance.md)에 있다.

## 15. 한계

- DB 조회 기반 대화로 실시간 알림은 없다.
- 메모리 rate limiter는 다중 인스턴스에서 공유되지 않는다.
- 이메일 인증/재설정, 관리자 MFA, AV 격리 스캔, 운영 TLS/HSTS 자동화는 없다.
- JPEG/WebP 정상 fixture, intent 시간 만료/일일 한도, 상품 optimistic lock의 전용 자동 테스트는 보강 대상이다.
- 실제 브라우저 수동 접근성/모바일 QA와 외부 보안 스캔은 아직 수행하지 않았다.

## 16. 결론

필수 일곱 기능군을 서버 화면과 Service/DB 규칙으로 구현했고 PostgreSQL에서 30개 자동 테스트를 통과했다. 주요 보안 경계는 URL 숨김이 아니라 세션, DTO, 정규화 식별자, 관계 조건 조회, 이미지 재인코딩, 감사 로그, DB 제약과 송금 트랜잭션으로 구성했다. 공개 전 민감정보 패턴과 제외 파일을 검사하고 보안 제보 URL을 실제 저장소 경로로 설정했다. 저장소 소유자는 GitHub 보안 설정과 원격 CI 결과를 최종 확인해야 한다.

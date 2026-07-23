# 요구사항 추적표

## 과제 최소 요구사항 판정

| ID | 과제 요구사항 | 사용 화면/기능 | 서버·DB 강제 조건 | 주요 자동 테스트 | 판정 |
|---|---|---|---|---|---|
| A-01 | 사람들이 플랫폼에 가입할 수 있어야 함 | `/signup`, `/login`, `/profile` | Argon2id, 이메일·닉네임 NFKC 정규화와 UNIQUE, 가입 시 지갑 생성 | `AuthenticationIntegrationTest` | 충족 |
| A-02 | 상품을 올리고 볼 수 있어야 함 | `/products/new`, `/products`, `/products/{id}`, 내 상품 수정·삭제·상태 변경 | 판매자 소유권 검사, 소프트 삭제, 이미지 검증·재인코딩 | `ProductIntegrationTest`, `ImageUploadIntegrationTest` | 충족 |
| A-03 | 플랫폼 사용자끼리 소통할 수 있어야 함 | 상품별 구매자·판매자 1:1 대화, 메시지 목록·읽음 | 참여자 조건 조회, 일반 텍스트, 30초 10건 제한, 대화 조합 UNIQUE | `ChatIntegrationTest` | 충족 |
| A-04 | 악성 사용자나 상품을 차단해야 함 | 상품·판매자·상대 사용자·메시지 신고, 관리자 사용자 정지/차단, 상품·메시지 숨김 | 신고 대상 권한/24시간 중복 검사, 차단 세션 만료·지갑 동결, 관리 사유·감사 로그 | `ReportIntegrationTest`, `AdminIntegrationTest` | 충족 |
| A-05 | 사용자 간 송금이 가능해야 함 | `/wallet/transfer` 2단계 가상 포인트 송금과 내역 | 유일 닉네임 수신자, 비밀번호 재확인, intent, 행 잠금, idempotency, 복식 원장 | `WalletConcurrencyIntegrationTest` | 충족 |
| A-06 | 상품을 검색할 수 있어야 함 | 키워드·카테고리·최소/최대 가격·정렬·페이지 검색 | JPA 파라미터 바인딩, 정렬 allowlist, 가격/페이지 경계 | `ProductIntegrationTest` | 충족 |
| A-07 | 관리자가 플랫폼의 모든 요소를 관리할 수 있어야 함 | 관리자 대시보드에서 사용자·상품·대화/메시지·신고·카테고리·송금·감사 관리 | `/admin/**` ADMIN 전용, 대화 열람 사유와 감사, 마지막 관리자 보호 | `AdminIntegrationTest` | 충족 |

송금 완료 내역과 복식 원장, 감사 로그는 무결성 보존을 위해 관리자도 수정·삭제하지 못하고 조회만 할 수 있다. 이는 관리 기능 누락이 아니라 회계·감사 자료의 변조를 막는 보안 설계다.

## 제출 품질 요구사항

| ID | 요구사항 | 구현 | 검증 | 상태 |
|---|---|---|---|---|
| Q-01 | 환경 설정·실행 방법 | `README.md`, `.env.example`, Docker Compose | 실행 JAR + PostgreSQL 스모크 테스트 | 완료 |
| Q-02 | 개발 과정·설계·테스트·보안·유지보수 보고 | `docs/`, `SECURITY.md` | 요구사항 추적과 문서 교차 점검 | 완료 |
| Q-03 | GitHub 공개 준비 | `.github`, `.gitignore`, `LICENSE` | CI/CodeQL/Dependabot 구성 점검 | 준비 완료 |

## 보안 요구사항 연결

| 위험 | 예방/검증 |
|---|---|
| CSRF | Spring Security 기본 CSRF, POST 상태 변경, 무토큰 로그아웃 회귀 테스트 |
| Stored XSS | 일반 텍스트 저장과 `th:text`, CSP, 상품/메시지 XSS 테스트 |
| SQL Injection | JPA/파라미터 바인딩, Specification, SQL 형태 검색어 테스트 |
| IDOR | 판매자/참여자/intent 소유자 조건 조회, 타 사용자 변경/대화/intent 테스트 |
| Mass Assignment | 요청 DTO allowlist, `role`/`balance` 추가 파라미터 테스트 |
| 무차별 대입/열거 | 일반화 실패 메시지, 실패 카운트와 임시 잠금 테스트 |
| 닉네임 중복/송금 수신자 오인 | NFKC·소문자 정규화, 가입·프로필 사전 검사, PostgreSQL UNIQUE, 필드 오류, 중복 회귀 테스트 |
| 업로드 우회 | 이름/확장자/MIME/시그니처/디코딩/크기/픽셀/개수 검사 테스트 |
| 송금 경쟁/중복 | intent, 재인증, unique key, 행 잠금, 순차·병렬·rollback 테스트 |
| 관리자 오용 | ADMIN 경로, 사유, 전후 상태, 감사 로그, 마지막 관리자 테스트 |
| 관리자 대화 과잉 열람 | 대시보드 진입점은 제공하되 본문 열람은 POST+CSRF와 5자 이상 사유를 요구하고 감사 로그 기록 |

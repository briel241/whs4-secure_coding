# 프로젝트 상태

마지막 갱신: 2026-07-24

## 현재 단계

M7 제출 준비 — 코드/자동 테스트/문서 완료, GitHub 공개 승인 완료

## 완료

- 빈 작업 폴더와 Git 미초기화 상태 확인
- `AGENTS.md`, `outputs/PROJECT_BLUEPRINT_KO.md` 부재 확인
- 첨부 요구사항 전체 검토
- Java/Docker/Gradle 로컬 도구 상태 조사
- 구현 계획 및 핵심 설계 결정 작성
- 안정 버전 확인 및 버전 고정
- M0 Spring Boot/Gradle/PostgreSQL/Flyway/공통 오류 기반
- M1 가입/Argon2id/로그인 제한/세션/프로필/탈퇴
- M2 상품 CRUD/검색/페이지/이미지 검증과 재인코딩
- M3 1:1 대화/메시지/읽음/429
- M4 신고/관리자/감사 로그/세션 무효화
- M5 2단계 송금/행 잠금/idempotency/복식 원장/대조
- M6 보안 회귀/실제 PostgreSQL 동시성 테스트
- M7 README/설계/DB/테스트/보안/유지보수/최종 보고서
- GitHub Actions, CodeQL, Dependabot, 템플릿, SECURITY, LICENSE
- 최종 `bootJar` 생성 및 PostgreSQL 17.10 대상 실행 스모크 테스트
- 비밀값 패턴, 필수 파일, Git 미초기화 상태 점검
- 라임/화이트 기반 반응형 UI, Pretendard Variable, 원화 가격 입력·표시 개선
- 가입·프로필 정규화 닉네임 중복 차단, 필드 오류 UI, 송금 식별 정합성 문서화
- 상품·대화 화면의 사용자/메시지 신고 진입점, 관리자 메시지 조사 메뉴와 신고 대상 바로가기
- 관리자 대화 열람을 GET 부수효과에서 POST+CSRF 방식으로 변경
- 관리자 사용자·상품·신고·송금·감사 목록 페이지 이동과 신고 대상 상품 전용 관리 화면
- 공개 전 민감정보 검사와 `briel241/whs4-secure_coding` 보안 제보 URL 설정

## 미완료

- 실제 브라우저 수동 접근성/반응형 QA
- 외부 의존성 취약점 DB 스캔과 GitHub CodeQL 실행
- GitHub Actions CI/CodeQL의 원격 실행 결과 확인

## 테스트 결과

- Java main/test 소스 컴파일: 성공
- Testcontainers PostgreSQL 17.10: 30/30 통과
- Flyway V1 + JPA `ddl-auto=validate`: 성공
- 송금 순차/병렬 idempotency, 잔액 경쟁, 반대 방향, DB 중간 실패 rollback: 통과
- 실행 JAR + Docker Compose PostgreSQL 17.10 기동: 성공

## 실행 제약

- 로컬 JDK는 23.0.1이다. 빌드는 Java 21 release/toolchain으로 제한했다.
- Docker Desktop 29.6.1 daemon을 시작해 Testcontainers 검증을 완료했다.
- PostgreSQL 18.4 기동 시 Flyway 공식 시험 범위 경고를 확인해 지원되는 PostgreSQL 17.10과 전용 volume으로 조정했다.

## 남은 위험

- 메모리 rate limiter는 다중 인스턴스 공유가 되지 않는다.
- 브라우저 수동 QA, JPEG/WebP fixture, intent 만료/일일 한도, optimistic lock 전용 테스트가 남았다.
- GitHub Secret scanning, Push protection, Private Vulnerability Reporting은 저장소 설정에서 소유자가 확인해야 한다.

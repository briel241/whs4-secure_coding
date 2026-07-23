# Tiny Second-hand Shopping Platform

서버 렌더링과 서버 세션을 사용하는 교육용 중고거래 플랫폼입니다. 회원가입, 상품/이미지, 검색, 1:1 대화, 신고/관리, 교육용 가상 포인트 송금을 하나의 Spring Boot 애플리케이션으로 제공합니다.

> **가상 포인트는 실제 돈이 아닙니다.** 결제, 현금화, 환전할 수 없으며 계좌번호, 카드번호, 주민등록번호 등 실제 금융정보를 수집하지 않습니다.

## 기능

- 정규화된 이메일/닉네임 가입, 전역 닉네임 중복 차단, Argon2id 비밀번호, 로그인 실패 잠금, POST 로그아웃
- 상품 등록/수정/상태/소프트 삭제, 공개 검색·필터·정렬·페이지네이션
- JPEG/PNG/WebP 검증과 안전한 PNG 재인코딩, UUID 저장 키, 전용 이미지 응답
- 상품별 구매자·판매자 1:1 대화, 읽음 상태, 일반 텍스트 메시지, 전송 제한
- 상품 상세와 대화 화면에서 사용자/상품/메시지 신고, 내 신고 상태
- 관리자 대시보드에서 사용자 상태, 상품/메시지 공개 상태, 신고, 카테고리, 송금, 감사 로그 관리
- 3분 송금 intent, 비밀번호 재확인, PostgreSQL 행 잠금, idempotency, 복식 원장
- Flyway 스키마, Testcontainers PostgreSQL 통합/동시성 테스트, GitHub Actions/CodeQL/Dependabot

## 고정 기술 버전

| 항목 | 버전 |
|---|---:|
| Java source/target | 21 |
| Spring Boot | 3.5.16 |
| Gradle Wrapper | 8.14.4 |
| PostgreSQL | 17.10 |
| Testcontainers | 1.21.4 |
| Bouncy Castle | 1.80 |
| TwelveMonkeys WebP | 3.12.0 |

로컬 검증 컴퓨터에는 JDK 23만 설치되어 있어 `--release 21`로 컴파일했습니다. GitHub Actions는 실제 Temurin 21에서 테스트하고 실행 JAR을 만듭니다.

## 디자인과 글꼴

형광 라임 포인트, 흰색 액션 버튼, 넓은 여백과 둥근 카드 중심의 반응형 UI를 사용합니다. 토스 전용 서체를 복제하지 않고, 시스템에 `Toss Product Sans`가 설치되어 있으면 우선 사용하며 그 외 환경에서는 자체 호스팅한 Pretendard Variable 1.3.9로 표시합니다. Pretendard의 SIL Open Font License 전문은 [`src/main/resources/static/fonts/OFL.txt`](src/main/resources/static/fonts/OFL.txt)에 포함했습니다.

## 사전 요구사항

- JDK 21
- Docker Desktop 또는 Docker Engine과 Compose v2
- Windows PowerShell 또는 macOS/Linux 셸
- 테스트 시 Docker daemon 실행

별도의 Gradle 설치는 필요하지 않습니다.

## 환경 변수

`.env.example`은 이름과 예시만 제공합니다. `.env`는 Git에서 제외되며 Spring Boot가 자동으로 읽지 않으므로 실행 셸에도 필요한 값을 설정해야 합니다.

| 변수 | 기본값/용도 |
|---|---|
| `POSTGRES_PASSWORD` | Compose PostgreSQL 비밀번호 |
| `POSTGRES_PORT` | `5432` |
| `DB_URL` | `jdbc:postgresql://localhost:5432/tinyshop` |
| `DB_USERNAME` | `tinyshop` |
| `DB_PASSWORD` | 로컬 기본값 `local-dev-password`; 운영에서는 필수 교체 |
| `UPLOAD_DIR` | `./var/uploads`; 웹 실행 경로 밖으로 지정 권장 |
| `SESSION_SECURE` | 로컬 HTTP `false`, 운영 HTTPS `true` |
| `SPRING_PROFILES_ACTIVE` | `demo` 또는 `prod` |
| `APP_BOOTSTRAP_ADMIN` | 일회성 관리자 부트스트랩 활성화 |
| `APP_ADMIN_EMAIL/NICKNAME/PASSWORD` | 부트스트랩 관리자 정보 |

## Windows 실행

```powershell
$env:POSTGRES_PASSWORD = "로컬에서만-사용할-긴-비밀번호"
$env:DB_PASSWORD = $env:POSTGRES_PASSWORD
$env:DB_URL = "jdbc:postgresql://localhost:5432/tinyshop"
$env:DB_USERNAME = "tinyshop"
$env:UPLOAD_DIR = ".\var\uploads"

docker compose up -d
.\gradlew.bat bootRun
```

브라우저에서 `http://localhost:8080`을 엽니다. 종료 후 DB만 멈추려면 `docker compose stop`을 사용합니다. `docker compose down -v`는 로컬 DB 볼륨을 삭제하므로 데이터 삭제 의도가 명확할 때만 실행하세요.

## macOS/Linux 실행

```bash
export POSTGRES_PASSWORD='로컬에서만-사용할-긴-비밀번호'
export DB_PASSWORD="$POSTGRES_PASSWORD"
export DB_URL='jdbc:postgresql://localhost:5432/tinyshop'
export DB_USERNAME='tinyshop'
export UPLOAD_DIR='./var/uploads'

docker compose up -d
./gradlew bootRun
```

Unix에서 실행 권한이 없다면 한 번만 `chmod +x gradlew`를 적용하세요.

## Docker Compose

Compose는 `postgres:17.10-alpine`, DB `tinyshop`, 사용자 `tinyshop`, major-version 전용 named volume을 사용합니다.

```bash
docker compose config
docker compose up -d
docker compose ps
```

애플리케이션 자체는 Docker 이미지로 묶지 않았습니다. 개발 과제의 단순한 실행과 디버깅을 위해 PostgreSQL만 Compose로 제공합니다.

## 애플리케이션 빌드와 실행

```bash
./gradlew bootJar
java -jar build/libs/tinyshop-1.0.0.jar
```

Windows에서는 `./gradlew` 대신 `.\gradlew.bat`를 사용합니다. 시작 시 Flyway가 빈 PostgreSQL에 `V1__initial_schema.sql`을 적용하고 Hibernate는 `ddl-auto=validate`로 매핑만 검증합니다.

## 테스트

Docker daemon을 먼저 실행한 뒤:

```bash
./gradlew test --no-build-cache
```

테스트는 H2를 사용하지 않습니다. Testcontainers가 `postgres:17.10-alpine`을 시작하여 Flyway/JPA, 권한, 업로드, 송금 잠금과 동시성을 검증합니다. 로컬 OneDrive 폴더가 Gradle 산출물을 잠그는 경우:

```powershell
$env:TINYSHOP_BUILD_DIR = Join-Path $env:TEMP "tinyshop-build"
.\gradlew.bat test --no-build-cache
```

최근 검증 결과: PostgreSQL 통합 테스트 포함 **30/30 통과**. 상세 체크리스트는 [docs/testing.md](docs/testing.md)에 있습니다.

## demo 프로필

demo는 실제 데이터가 아닌 제출/학습용 계정을 만들며 `app.demo.initial-points`만큼 가상 포인트를 원장 `GRANT`로 기록합니다.

```powershell
$env:SPRING_PROFILES_ACTIVE = "demo"
$env:DEMO_ADMIN_EMAIL = "admin@example.invalid"
$env:DEMO_ADMIN_PASSWORD = "demo-only-admin-password"
$env:DEMO_USER_PASSWORD = "demo-only-user-password"
.\gradlew.bat bootRun
```

- 관리자: `DEMO_ADMIN_EMAIL`
- 판매자: `seller@example.invalid`
- 구매자: `buyer@example.invalid`
- 비밀번호는 반드시 환경 변수로 교체하세요.

demo 프로필을 운영에서 사용하지 마세요.

## 관리자 생성

운영/일반 프로필에서 최초 관리자는 일회성 환경 변수로 생성합니다.

```powershell
$env:APP_BOOTSTRAP_ADMIN = "true"
$env:APP_ADMIN_EMAIL = "admin@example.invalid"
$env:APP_ADMIN_NICKNAME = "운영관리자"
$env:APP_ADMIN_PASSWORD = "충분히-긴-임시-비밀번호"
.\gradlew.bat bootRun
```

생성을 확인한 즉시 애플리케이션을 종료하고 `APP_BOOTSTRAP_ADMIN=false`로 되돌린 뒤 재시작하세요. 이미 같은 이메일의 일반 계정이 있으면 역할을 승격하지 않고 시작을 거부합니다. 비밀번호를 명령 기록이나 저장소에 남기지 마세요.

## 보안 설계 요약

- 기본 거부 URL 정책과 Service/Repository의 소유자·참여자 조건
- 서버 세션, 세션 고정 방어, HttpOnly/SameSite 쿠키, 운영 Secure 쿠키
- CSRF 기본 활성화와 POST 로그아웃/관리/송금
- Argon2id, 사용자 열거 완화, 5회 실패 후 15분 임시 잠금
- 가입·프로필 변경 모두 NFKC/소문자 정규화 닉네임을 사전 검사하고 PostgreSQL UNIQUE로 최종 차단
- DTO allowlist로 role/status/balance mass assignment 차단
- Thymeleaf `th:text` 일반 텍스트 출력, CSP/`nosniff`/DENY/no-referrer
- 이미지 다중 검증과 재인코딩, 외부 저장 경로
- 파라미터 바인딩/JPA Specification, 정렬 allowlist, 최대 페이지 크기
- 송금 intent 소유권, 재인증, 결정적 행 잠금, unique idempotency, 복식 원장
- 관리자 사유/전후 상태/결과 감사 기록, 마지막 관리자 보호

자세한 내용은 [docs/security-review.md](docs/security-review.md)와 [SECURITY.md](SECURITY.md)에 있습니다.

## 알려진 제한

- WebSocket 없이 화면 새로고침 기반 대화입니다.
- 실제 결제·은행·환전·배송 연동이 없습니다.
- 로그인/메시지 제한 상태는 단일 애플리케이션 인스턴스 메모리를 일부 사용하므로 수평 확장 전 공유 저장소 또는 DB 기반 제한기로 교체해야 합니다.
- 이미지 악성코드 스캔/EXIF 제거 정책은 재인코딩으로 완화했지만 전용 안티바이러스와 격리 저장소는 포함하지 않았습니다.
- 이메일 인증, 비밀번호 재설정, 관리자 MFA는 범위 밖입니다.
- 관리자 대화 본문 조사는 대시보드 또는 메시지 신고 상세에서 진입하며, 매번 조사 사유를 남겨야 합니다.
- 외부 의존성 취약점 DB와 GitHub CodeQL의 실제 실행 결과는 저장소 Actions에서 별도로 확인해야 합니다.

## 문서

- [구현 계획](docs/IMPLEMENTATION_PLAN.md)
- [상태](docs/STATUS.md)
- [설계 결정](docs/DECISIONS.md)
- [요구사항 추적](docs/requirements.md)
- [아키텍처](docs/architecture.md)
- [데이터베이스](docs/database.md)
- [테스트](docs/testing.md)
- [보안 검토](docs/security-review.md)
- [유지보수](docs/maintenance.md)
- [최종 개발 보고서](docs/REPORT_KO.md)

## GitHub 저장소

소스는 [briel241/whs4-secure_coding](https://github.com/briel241/whs4-secure_coding)에 공개합니다. 공개 전 `.env`, 로그, 업로드, 인증서, 실제 개인정보가 추적되지 않았는지 검사했고 보안 제보 URL도 실제 저장소 경로로 설정했습니다.

저장소 소유자는 GitHub 설정에서 Secret scanning, Push protection, Private Vulnerability Reporting과 Actions의 CI/CodeQL·Dependabot 권한을 확인해야 합니다.

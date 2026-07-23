# 보안 검토

## 1. 설계 단계에서 예방한 위험

- JWT/localStorage와 SPA를 사용하지 않고 서버 세션/SSR로 토큰 노출 면적을 축소했다.
- Entity 직접 바인딩을 금지하고 DTO allowlist를 사용했다.
- UUID와 별개로 seller/participant/user/intent 관계를 권한 경계로 삼았다.
- 클라이언트 hidden 금액을 받지 않고 DB intent만 송금 근거로 사용했다.
- 완료 원장/감사 기록의 수정·삭제 경로를 설계하지 않았다.

## 2. 구현 중 코드 리뷰로 발견하고 수정한 약점

| 발견 | 수정 전 위험 | 수정 |
|---|---|---|
| 로그인 전 이메일 공백이 Bean Validation에서 먼저 거부됨 | 요구한 정규화 동작 불일치 | 이메일 필드만 검증 전 trim, 비밀번호는 원문 유지 |
| Testcontainers가 클래스별 종료되고 컨텍스트가 주소를 재사용 | 이후 보안/동시성 테스트가 DB 없이 실패 | JVM 수명의 단일 PostgreSQL 컨테이너와 동적 속성 사용 |
| Gradle batch가 실패 코드를 숨김 | CI/로컬이 실패를 성공으로 오인 | `%ERRORLEVEL%` 보존 후 반환 |
| 업로드 정리 실패가 조용히 무시됨 | 잔여 파일 조사 곤란 | 저장 키만 WARN, 원본명/내용은 로그 금지 |
| CSS 외부 폰트 import | CSP 위반과 외부 요청 | 외부 import 제거, OFL Pretendard를 로컬 자체 호스팅 |
| 닉네임 중복 오류가 가입 폼 전체 오류로만 표시됨 | 사용자가 충돌 필드를 알기 어렵고 송금 식별자 정합성 확인이 약함 | 필드별 예외, 빨간 테두리, `aria-invalid`, 명시적 오류 문구 |
| 프로필 닉네임 변경이 커밋 시점까지 flush되지 않음 | 동시 중복 요청의 DB 제약 오류가 폼 응답 밖으로 전파될 수 있음 | 트랜잭션 안에서 즉시 flush하고 안전한 필드 오류로 변환 |

## 3. 테스트에서 실제 발견한 약점

- 첫 PostgreSQL 실행에서 컨테이너 수명주기 문제와 이메일 trim 순서 문제를 실제 실패로 발견했다.
- 수정 후 전체 30개 테스트를 재실행해 회귀 통과했다.
- 대소문자와 공백 차이만 있는 정규화 중복 닉네임을 가입·프로필 변경 양쪽에서 거부하고, 원래 프로필이 유지되는지 검증했다.
- 제품 송금 불변조건 위반은 테스트에서 발견되지 않았다. 인위적 DB 원장 실패는 잔액/송금/intent가 함께 rollback됨을 확인했다.

## 4. 적용한 방어

- CSRF, POST 로그아웃, 세션 fixation migration, HttpOnly/SameSite, prod Secure
- Argon2id와 12~128자, 실패 5회/15분 잠금, 일반화 로그인 실패
- NFKC·소문자 닉네임 정규화, 애플리케이션 중복 검사, PostgreSQL UNIQUE, 송금 수신자 단건 조회
- CSP, frame deny, nosniff, no-referrer, permissions policy
- `th:text`, 일반 텍스트, 로그 개행 정리, 메시지 본문 비로그
- JPA/바인딩과 정렬 allowlist/페이지 상한
- 파일 서명/디코딩/재인코딩/경로 격리
- 관리자 사유/전후 상태/결과/시간/actor 감사
- PostgreSQL CHECK/UNIQUE/FK와 결정적 `FOR UPDATE`

## 5. 아직 남은 위험

- 단일 인스턴스 메모리 rate limiter는 분산 환경에서 우회될 수 있다.
- AV/CDR, 콘텐츠 moderation 자동화, EXIF 전문 정책은 없다.
- 이메일 인증/재설정, MFA, 관리자 IP 정책은 없다.
- demo 기본 예시 비밀번호는 반드시 환경 변수로 교체해야 한다.
- 브라우저/프록시/TLS/HSTS는 실제 운영 배포 환경에서 별도 검증이 필요하다.
- 로컬 의존성 취약점 DB 스캔은 실행하지 않았다. Dependabot, dependency submission, CodeQL은 저장소 공개 후 동작한다.
- NFKC는 대소문자·전각 문자 등 정규화 중복을 막지만 서로 다른 유니코드 문자의 시각적 유사성 전체를 판별하지는 않는다. 운영 정책에서 허용 문자 집합을 더 좁힐 수 있다.

현재 코드 리뷰와 실행 테스트 범위에서 미해결 Critical/High 문제는 확인되지 않았지만, 이는 제3자 침투 테스트나 운영 보안 인증을 의미하지 않는다.

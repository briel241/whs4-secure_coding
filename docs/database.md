# 데이터베이스 설계

Flyway `V1__initial_schema.sql`이 전체 초기 스키마를 만든다. JPA는 `ddl-auto=validate`만 사용한다.

| 테이블 | 목적 | 핵심 무결성 |
|---|---|---|
| `users` | 계정/역할/상태/로그인 실패 | 정규화 이메일·닉네임 unique, 상태 CHECK |
| `categories` | 상품 분류 | 정규화 이름 unique, 활성 플래그 |
| `products` | 상품과 판매/관리 상태 | seller/category FK, 가격/상태 CHECK, version |
| `product_images` | 재인코딩 이미지 메타데이터 | storage key unique, 상품당 display order unique |
| `conversations` | 상품별 1:1 대화 | product+buyer+seller unique, 자기 대화 CHECK |
| `messages` | 일반 텍스트 메시지 | 길이 CHECK, hidden 소프트 상태 |
| `conversation_reads` | 사용자별 마지막 읽음 | conversation+user unique |
| `wallet_accounts` | 정수 포인트 잔액 | user unique, balance >= 0, version |
| `transfer_intents` | 3분 송금 준비 | idempotency unique, 금액/상태 CHECK |
| `transfers` | 완료/정정 송금 | intent unique, sender+idempotency unique |
| `wallet_ledger_entries` | 불변 원장 | transfer당 wallet unique, signed amount != 0 |
| `reports` | 신고와 처리 상태 | 유형/상태 CHECK, reporter FK |
| `moderation_actions` | 관리 전후 상태/사유 | admin FK, 사유 필수 |
| `audit_logs` | 구조화된 감사 | actor/target/result/reason |

## 닉네임 유일성과 송금 식별

`users.normalized_nickname`은 NFKC 정규화, 앞뒤 공백 제거, 소문자 변환 결과를 저장하며 UNIQUE 제약을 가진다. 가입과 프로필 수정은 저장 전에 중복을 검사해 필드 오류를 반환하고, 동시에 같은 닉네임을 요청하는 경합은 PostgreSQL UNIQUE가 최종 차단한다. 프로필 변경은 트랜잭션 안에서 즉시 flush해 제약 위반을 사용자 응답으로 안전하게 변환한다. 송금 수신자는 표시 닉네임이 아니라 이 유일한 정규화 값으로 한 명만 조회한다.

## 인덱스

공개 상품 상태/관리 상태/생성일, 판매자, 카테고리, 가격, 소문자 제목, 대화 참여자, 메시지 시간, 송금 발신/수신 시간, 신고 큐, 감사 시간에 인덱스를 둔다.

## 원장

정상 송금은 발신 `DEBIT` 음수와 수신 `CREDIT` 양수 두 행을 만들고 송금별 합이 0이다. demo 초기 포인트는 transfer가 없는 `GRANT` 원장으로 기록해 지갑별 원장 합과 잔액을 맞춘다. 완료 송금/원장/감사 로그의 수정·삭제 Repository 경로는 제공하지 않는다.

## 마이그레이션 원칙

공개/배포 후 `V1`은 수정하지 않는다. 변경은 `V2__...sql`처럼 새 파일로 추가하고, 깨끗한 DB와 기존 DB 업그레이드를 모두 테스트한다.

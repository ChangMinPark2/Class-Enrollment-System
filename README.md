# 🎓 수강 신청 관리 시스템 (Enrollment Management System)

## 📋 프로젝트 개요

강의 수강 신청, 결제 확정, 수강 취소, 대기열 처리를 지원하는 Spring Boot 기반 REST API 서버입니다.

특히 **동시성 상황에서의 데이터 정합성 보장**에 중점을 두어 설계되었습니다.

### 주요 기능

- 📌 수강 신청 생성 및 중복 신청 방지
- 💳 결제 완료 기반 수강 확정 처리
- ❌ 수강 취소 및 정원 감소 처리
- ⏳ 대기열 등록 및 승격 처리
- ⚡ DB 원자적 업데이트 기반 동시성 제어
- 🧪 단위 테스트 및 동시성 통합 테스트 검증

---

## 🛠 기술 스택

| 분류 | 기술 | 버전 |
|------|------|------|
| **Backend** | Spring Boot | 3.5.13 |
| **Language** | Java | 17 |
| **Database** | H2 (In-Memory) | Managed by Spring Boot |
| **Build Tool** | Gradle | 8.14.4 |
| **ORM** | Spring Data JPA | Managed by Spring Boot |
| **Test** | JUnit5, Mockito | - |

---

## 🚀 실행 방법

### 1. 프로젝트 클론

```bash
git clone https://github.com/ChangMinPark2/Class-Enrollment-System.git
cd Class-Enrollment-System
```

### 2. 애플리케이션 실행

```bash
./gradlew bootRun
```

- 기본적으로 `localhost:8080`에서 서버가 실행됩니다.

### 3. 테스트 실행

서버가 실행 중인 상태에서 **새 터미널을 열어** 아래 명령어를 실행합니다.

```bash
./gradlew clean test
```

> 테스트는 H2 인메모리 DB를 직접 띄우는 방식으로 동작하므로, 서버 실행 여부와 관계없이 독립적으로 실행됩니다.

테스트 결과 리포트는 아래 경로에서 확인할 수 있습니다.

```
build/reports/tests/test/index.html
```

Mac 환경에서는 다음 명령어로 바로 확인할 수 있습니다.

```bash
open build/reports/tests/test/index.html
```
---

## 📝 요구사항 해석 및 가정

### 사용자 생성

- 사용자는 이름과 역할(`Role`)을 기반으로 생성됩니다.
- 사용자 역할은 `CREATOR`, `STUDENT` 등으로 구분됩니다.
- 회원가입 시 별도의 인증 절차 없이 단순 생성으로 처리했습니다.
- 생성된 사용자는 이후 강의 생성 또는 수강 신청 등 역할에 맞는 기능을 수행할 수 있습니다.

### 사용자 역할 분리

- `CREATOR`: 강의를 생성하고, 오픈 및 종료를 관리할 수 있는 사용자
- `STUDENT`: 강의를 조회하고 수강 신청, 확정, 취소를 수행하는 사용자

### 강의 생성

- `CREATOR` 역할을 가진 사용자만 강의를 생성할 수 있습니다.
- 강의 생성 시 제목, 설명, 가격, 최대 수강 인원, 시작일, 종료일을 입력합니다.
- 강의 시작일과 종료일은 유효한 날짜 범위여야 합니다.
- 생성된 강의는 기본적으로 `DRAFT` 상태로 생성됩니다.
- `DRAFT` 상태의 강의는 아직 수강 신청을 받을 수 없습니다.

### 강의 오픈

- 강의 생성자만 본인이 생성한 강의를 오픈할 수 있습니다.
- 강의 상태가 오픈 가능한 상태일 때만 `OPEN` 상태로 변경할 수 있습니다.
- 종료일이 지난 강의는 오픈할 수 없습니다.
- `OPEN` 상태가 된 강의만 사용자가 수강 신청할 수 있습니다.

### 강의 종료

- 강의 생성자만 본인이 생성한 강의를 종료할 수 있습니다.
- 강의 상태가 종료 가능한 상태일 때만 `CLOSED` 상태로 변경할 수 있습니다.
- `CLOSED` 상태의 강의는 더 이상 수강 신청을 받을 수 없습니다.

### 수강 신청

- 사용자는 특정 강의에 수강 신청할 수 있습니다.
- 강의 상태가 `OPEN`인 경우에만 수강 신청할 수 있습니다.
- 강의 생성자는 본인이 생성한 강의에 수강 신청할 수 없습니다.
- 이미 해당 강의에 수강 신청한 사용자는 중복 신청할 수 없습니다.
- 강의 정원이 남아 있고 대기자가 없다면 수강 신청은 `PENDING` 상태로 생성됩니다.
- 강의 정원이 가득 찼거나 기존 대기자가 있다면 대기열에 등록됩니다.

### 수강 확정

- 수강 신청 상태가 `PENDING`인 경우에만 확정할 수 있습니다.
- 수강 확정은 결제 완료 상황으로 가정했습니다.
- 수강 확정 시 강의의 현재 수강 인원인 `currentCapacity`가 증가합니다.
- 여러 사용자가 동시에 확정 요청을 보내더라도 `maxCapacity`를 초과할 수 없습니다.
- 한 사용자가 동일한 수강 신청에 대해 중복 확정 요청을 보내도 한 번만 확정됩니다.

### 수강 취소

- 수강 신청 상태가 `CONFIRMED`인 경우에만 취소할 수 있습니다.
- 결제 확정 후 **7일 이내**에만 취소할 수 있습니다.
- 취소 성공 시 수강 신청 상태는 `CANCELLED`로 변경됩니다.
- 취소 성공 시 강의의 `currentCapacity`가 감소합니다.
- 한 사용자가 동일한 취소 요청을 여러 번 보내도 한 번만 취소됩니다.
- 서로 다른 사용자가 동시에 취소하는 경우 각자의 취소 요청은 정상적으로 반영됩니다.

### 대기열

- 강의 정원이 가득 찼거나 기존 대기자가 존재하면 사용자는 대기열에 등록됩니다.
- 대기열 상태는 `WAITING`, `PROMOTED`, `EXPIRED`, `COMPLETED`로 관리됩니다.
- 수강 취소가 발생하면 다음 대기자를 승격합니다.
- 승격된 사용자는 제한 시간 내에 수강 확정을 진행할 수 있습니다.
- 승격 시간이 만료되면 해당 대기열은 `EXPIRED` 처리되고 다음 대기자를 승격합니다.

---

## 💡 설계 결정과 이유

### 1. 정원 증가에 DB 원자적 업데이트 적용

수강 확정 시 현재 수강 인원 증가 로직은 다음과 같이 조건부 업데이트로 처리했습니다.

```sql
UPDATE course
SET current_capacity = current_capacity + 1
WHERE id = ?
AND current_capacity < max_capacity
```

이 방식을 선택한 이유는 다음과 같습니다.

- 조건 확인과 증가가 하나의 SQL 문에서 처리됨
- 동시에 여러 요청이 들어와도 정원을 초과하지 않음
- 비관적 락보다 락 점유 시간이 짧고 단순한 카운터 증가에 적합함

---

### 2. 수강 확정 상태 변경에 DB 원자적 업데이트 적용

한 사용자가 동일한 수강 신청에 대해 확정 요청을 여러 번 보내는 경우를 방지하기 위해 `PENDING` 상태일 때만 `CONFIRMED`로 변경하도록 처리했습니다.

```sql
UPDATE enrollment
SET enrollment_status = 'CONFIRMED',
    confirmed_at = ?
WHERE id = ?
AND enrollment_status = 'PENDING'
```

이를 통해 동일한 수강 신청이 중복 확정되는 문제를 방지했습니다.

---

### 3. 수강 취소 상태 변경에 DB 원자적 업데이트 적용

수강 취소 시에도 동일한 수강 신청이 여러 번 취소되는 문제를 방지하기 위해 `CONFIRMED` 상태일 때만 `CANCELLED`로 변경하도록 처리했습니다.

```sql
UPDATE enrollment
SET enrollment_status = 'CANCELLED'
WHERE id = ?
AND enrollment_status = 'CONFIRMED'
```

이를 통해 더블클릭 또는 중복 요청으로 인해 `currentCapacity`가 여러 번 감소하는 문제를 방지했습니다.

---

### 4. 정원 감소에도 DB 원자적 업데이트 적용

서로 다른 사용자가 동시에 수강 취소를 요청하는 경우, 엔티티의 값을 직접 감소시키면 Lost Update 문제가 발생할 수 있습니다.

따라서 다음과 같이 DB 원자적 업데이트를 사용했습니다.

```sql
UPDATE course
SET current_capacity = current_capacity - 1
WHERE id = ?
AND current_capacity > 0
```

이를 통해 현재 수강 인원이 0보다 작아지는 문제를 방지하고, 동시에 여러 취소 요청이 발생해도 정확한 수강 인원이 유지되도록 했습니다.

---

## ⚠️ 미구현 / 제약사항

- 실제 결제 시스템 연동은 구현하지 않았습니다.
- 결제 완료 상황은 `confirm` API 호출로 가정했습니다.
- 인증 및 인가 기능은 별도로 구현하지 않고, 요청 파라미터의 `userId`를 기준으로 사용자를 식별했습니다.
- 대기열 만료 시간은 서버 스케줄러를 통해 처리됩니다.
- 대기열 승격 알림 기능은 로그로 대체했습니다.

---

## 🤖 AI 활용 범위

본 과제에서는 AI를 다음 범위에서 활용했습니다.

- 요구사항 분석 및 구현 방향 검토
- 더 나은 서비스 구조, 책임 분리, 동시성 처리 방식에 대한 지속적인 검토
- 테스트 케이스 설계 방향 검토
- Mockito 기반 단위 테스트 코드 초안 작성 보조
- ExecutorService와 CountDownLatch를 활용한 동시성 통합 테스트 구조 검토
- 테스트 데이터 생성을 위한 엔티티 생성 헬퍼 메서드 등 반복 코드 작성 보조
- README 초안 작성 보조
- PR 공유 포인트 문장 정리

단, 핵심 비즈니스 로직의 요구사항 해석, 도메인 설계, 동시성 처리 방식 선택, 최종 코드 반영 및 검증은 직접 수행했습니다.

---

## 📡 API 목록 및 예시

> 모든 API는 `http://localhost:8080` 을 base URL로 사용합니다.

---

### 1. 회원가입

```
POST /api/users
```

**Request**

```json
{
  "name": "홍길동",
  "role": "CREATOR"
}
```

---

### 2. 강의 생성

```
POST /api/users/{userId}/courses
```

**Request**

```json
{
  "title": "스프링 부트 입문",
  "description": "스프링 기초부터 실습까지 배우는 강의입니다.",
  "price": 10000,
  "maxCapacity": 1,
  "startedAt": "2026-04-25",
  "endedAt": "2026-05-25"
}
```

---

### 3. 강의 오픈

```
POST /api/users/{userId}/courses/{courseId}/open
```

**Response**

```json
{
  "message": "강의가 오픈되었습니다."
}
```

---

### 4. 강의 마감

```
POST /api/users/{userId}/courses/{courseId}/close
```

**Response**

```json
{
  "message": "강의가 마감되었습니다."
}
```

---

### 5. 강의 목록 조회

```
GET /api/courses
```

**Response**

```json
{
  "courses": [
    {
      "id": 1,
      "title": "스프링 부트 입문",
      "price": 10000,
      "maxCapacity": 1,
      "currentCapacity": 0,
      "status": "CLOSED"
    }
  ]
}
```

---

### 6. 강의 단건 조회

```
GET /api/courses/{courseId}
```

**Response**

```json
{
  "id": 1,
  "title": "스프링 부트 입문",
  "description": "스프링 기초부터 실습까지 배우는 강의입니다.",
  "price": 10000,
  "maxCapacity": 1,
  "currentCapacity": 0,
  "startedAt": "2026-04-25",
  "endedAt": "2026-05-25",
  "status": "CLOSED"
}
```

### 7. 수강 신청

```
POST /api/enrollments
```

**Request**

```json
{
  "userId": 2,
  "courseId": 1
}
```

**Response** (둘 중 하나가 반환됩니다)

```json
{
  "type": "ENROLLMENT",
  "message": "수강 신청이 완료되었습니다. 결제를 진행해주세요."
}
```

```json
{
  "type": "WAITLIST",
  "message": "대기열에 등록되었습니다. 빈 자리가 생기면 수강 신청 기회가 제공됩니다."
}
```

---

### 8. 결제 (수강 확정)

```
POST /api/enrollments/{enrollmentId}/confirm?userId={userId}
```

**Response** (둘 중 하나가 반환됩니다)

```json
{
  "type": "CONFIRMED",
  "message": "결제가 완료되어 수강이 확정되었습니다."
}
```

```json
{
  "type": "WAITLIST",
  "message": "대기열에 등록되었습니다. 빈 자리가 생기면 수강 신청 기회가 제공됩니다."
}
```

---

### 9. 수강 취소

```
POST /api/enrollments/{enrollmentId}/cancel?userId={userId}
```

**Response**

```json
{
  "message": "수강 신청이 취소되었습니다."
}
```

### 10. 내 수강 신청 목록 조회

```
GET /api/users/{userId}/enrollments?page={page}&size={size}
```

**Response**

```json
{
  "enrollments": [
    {
      "enrollmentId": 1,
      "courseId": 1,
      "courseTitle": "스프링 부트 입문",
      "price": 10000,
      "status": "CONFIRMED",
      "confirmedAt": "2026-04-27T00:11:53.018934"
    },
    {
      "enrollmentId": 2,
      "courseId": 2,
      "courseTitle": "스프링 부트 입문",
      "price": 10000,
      "status": "PENDING",
      "confirmedAt": null
    }
  ],
  "page": 0,
  "size": 2,
  "totalElements": 2,
  "totalPages": 1
}
```

---

### 11. 강의별 수강생 목록 조회 (강사 전용)

```
GET /api/users/{userId}/courses/{courseId}/students
```

**Response**

```json
{
  "students": [
    {
      "userId": 2,
      "userName": "홍길동",
      "enrollmentId": 1,
      "status": "CONFIRMED",
      "confirmedAt": "2026-04-27T03:51:01.654985"
    },
    {
      "userId": 3,
      "userName": "홍길동",
      "enrollmentId": 2,
      "status": "CONFIRMED",
      "confirmedAt": "2026-04-27T03:51:56.122846"
    }
  ]
}
```

## 🗄 데이터 모델 설명

### ERD 구조

```
tbl_user
├── id (PK)
├── name
└── role (CREATOR | STUDENT)
        │
        ├──────────────────────────┐
        ▼                          ▼
tbl_course                   tbl_enrollment
├── id (PK)                  ├── id (PK)
├── title                    ├── user_id (FK → tbl_user)
├── description              ├── course_id (FK → tbl_course)
├── price                    ├── status (PENDING | CONFIRMED | CANCELLED)
├── max_capacity             └── confirmed_at
├── current_capacity                  │
├── started_at                        │ (수강 취소 시 대기열 승격)
├── ended_at                          ▼
├── status (DRAFT|OPEN|CLOSED)  tbl_waitlist
└── user_id (FK → tbl_user)     ├── id (PK)
                                ├── user_id (FK → tbl_user)
                                ├── course_id (FK → tbl_course)
                                ├── waitlist_status (WAITING | PROMOTED | EXPIRED | COMPLETED)
                                ├── created_at
                                ├── promoted_at
                                └── expires_at
```

---

### 엔티티 설명

**👤 User** `tbl_user`

사용자 정보를 저장합니다. `role`에 따라 강의를 생성하는 강사(`CREATOR`)와 수강 신청하는 학생(`STUDENT`)으로 구분됩니다.

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT (PK) | 사용자 ID |
| name | VARCHAR | 사용자 이름 |
| role | ENUM | 역할 (`CREATOR` / `STUDENT`) |

---

**📚 Course** `tbl_course`

강의 정보를 저장합니다. `status`를 통해 강의의 생애주기를 관리하며, `current_capacity`와 `max_capacity`를 비교해 수강 가능 여부를 판단합니다.

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT (PK) | 강의 ID |
| user_id | BIGINT (FK) | 강의 생성자 |
| title | VARCHAR(100) | 강의 제목 |
| description | VARCHAR(1000) | 강의 설명 |
| price | INT | 강의 가격 |
| max_capacity | INT | 최대 수강 인원 |
| current_capacity | INT | 현재 수강 인원 |
| started_at | DATE | 강의 시작일 |
| ended_at | DATE | 강의 종료일 |
| status | ENUM | 강의 상태 (`DRAFT` / `OPEN` / `CLOSED`) |

**CourseStatus**

| 상태 | 설명 |
|------|------|
| DRAFT | 생성 직후 기본 상태 |
| OPEN | 수강 신청 가능 상태 |
| CLOSED | 수강 신청 마감 상태 |

---

**📋 Enrollment** `tbl_enrollment`

수강 신청 정보를 저장합니다. 결제 대기(`PENDING`) → 수강 확정(`CONFIRMED`) 흐름으로 처리되며, 동시성 제어를 위해 상태 변경은 조건부 업데이트로 처리됩니다.

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT (PK) | 수강 신청 ID |
| user_id | BIGINT (FK) | 수강 신청 사용자 |
| course_id | BIGINT (FK) | 신청한 강의 |
| status | ENUM | 수강 신청 상태 |
| confirmed_at | DATETIME | 수강 확정 시각 |

**EnrollmentStatus**

| 상태 | 설명 |
|------|------|
| PENDING | 결제 대기 중 |
| CONFIRMED | 결제 완료, 수강 확정 |
| CANCELLED | 수강 취소 |

---

**⏳ Waitlist** `tbl_waitlist`

정원 초과 시 대기열 정보를 저장합니다. 수강 취소가 발생하면 `WAITING` 상태의 첫 번째 대기자를 `PROMOTED`로 승격하며, 제한 시간 내 확정하지 않으면 `EXPIRED` 처리 후 다음 대기자를 승격합니다.

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT (PK) | 대기열 ID |
| user_id | BIGINT (FK) | 대기 사용자 |
| course_id | BIGINT (FK) | 대기 강의 |
| waitlist_status | ENUM | 대기열 상태 |
| created_at | DATETIME | 대기열 등록 시각 |
| promoted_at | DATETIME | 승격 시각 |
| expires_at | DATETIME | 승격 만료 시각 |

**WaitlistStatus**

| 상태 | 설명 |
|------|------|
| WAITING | 대기 중 |
| PROMOTED | 수강 신청 기회 부여됨 |
| EXPIRED | 제한 시간 내 미확정으로 만료 |
| COMPLETED | 수강 확정 완료 |

## 🧪 테스트 실행 방법

전체 테스트는 다음 명령어로 실행할 수 있습니다.

```bash
./gradlew clean test
```

---

### 단위 테스트

Service 계층의 주요 비즈니스 로직을 Mockito 기반 단위 테스트로 검증했습니다.

검증 대상은 다음과 같습니다.

- 강의 생성 성공 및 예외 케이스 (강의 생성자 권한 검증, 필수 값 누락 등)
- 강의 오픈 성공 및 예외 케이스 (본인 강의 여부 검증, 상태 전환 검증)
- 강의 마감 성공 및 예외 케이스 (본인 강의 여부 검증, 상태 전환 검증)
- 수강 신청 성공 및 예외 케이스 (정원 초과, 중복 신청, 본인 강의 신청 방지 등)
- 수강 확정 성공 및 예외 케이스 (상태 검증, 정원 초과 방지 등)
- 수강 취소 성공 및 예외 케이스 (상태 검증, 본인 신청 건 여부 검증 등)
- 대기열 등록, 승격, 만료 처리
- 조회 서비스 성공 및 예외 케이스

---

### 통합 테스트

동시성 상황은 실제 Spring Context와 Repository를 사용하는 통합 테스트로 검증했습니다.

---

#### ✅ Confirm 동시성 테스트

**1. 한 사용자의 동시 요청 (더블클릭)**

한 사용자가 동일한 수강 신청에 대해 확정 요청을 동시에 여러 번 보내는 상황을 검증했습니다.
`PENDING → CONFIRMED` 상태 변경을 DB 원자적 업데이트(`confirmIfPending`)로 처리하여 중복 확정을 방지했습니다.

| 항목 | 결과 |
|------|------|
| 수강 확정 성공 | 1번 |
| 나머지 요청 | 실패 |
| currentCapacity 증가 | 1번만 증가 |
| 중복 확정 여부 | 발생하지 않음 |

**2. 여러 사용자의 동시 요청**

정원이 1개만 남은 강의에 여러 사용자가 동시에 수강 확정 요청을 보내는 상황을 검증했습니다.
`currentCapacity < maxCapacity` 조건을 포함한 DB 원자적 업데이트를 통해 정원 초과를 방지했습니다.

| 항목 | 결과 |
|------|------|
| 수강 확정 성공 | 1명 |
| 나머지 요청 | 대기열 등록 |
| currentCapacity | maxCapacity 초과하지 않음 |

---

#### ✅ Cancel 동시성 테스트

**1. 한 사용자의 동시 요청 (더블클릭)**

한 사용자가 동일한 수강 신청 건에 대해 취소 요청을 동시에 여러 번 보내는 상황을 검증했습니다.
`CONFIRMED → CANCELLED` 상태 변경을 DB 원자적 업데이트(`cancelIfConfirmed`)로 처리하여 중복 취소를 방지했습니다.

| 항목 | 결과 |
|------|------|
| 취소 성공 | 1번 |
| 나머지 요청 | 실패 |
| currentCapacity 감소 | 1번만 감소 |
| currentCapacity 음수 여부 | 발생하지 않음 |

**2. 여러 사용자의 동시 요청**

서로 다른 사용자가 각각 본인의 수강 신청 건을 동시에 취소하는 상황을 검증했습니다.
`currentCapacity` 감소를 DB 원자적 업데이트로 처리하여 Lost Update 문제를 방지했습니다.

| 항목 | 결과 |
|------|------|
| 취소 성공 | 모든 요청 성공 |
| currentCapacity 감소 | 취소된 인원 수만큼 정확히 감소 |
| 동시성 정합성 | 유지됨 |

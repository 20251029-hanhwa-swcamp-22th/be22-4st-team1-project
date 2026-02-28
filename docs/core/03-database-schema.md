# MapLog 데이터베이스 스키마

> **한 줄 요약**: MapLog의 데이터베이스는 MariaDB 11을 사용하며, User, Diary, Friend, Notification 등 5개 주요 도메인 엔티티와 관계가 체계적으로 설계되어 있습니다.

## 목차 (Table of Contents)

- [개요](#개요)
- [테이블 상세 설명](#테이블-상세-설명)
- [엔티티 관계도](#엔티티-관계도)
- [인덱싱 전략](#인덱싱-전략)
- [제약 조건](#제약-조건)

---

## 개요

### 데이터베이스 정보

```yaml
DBMS: MariaDB 11
Charset: utf8mb4
Collation: utf8mb4_unicode_ci
```

### 테이블 목록

| 테이블명 | 용도 | 주요 칼럼 |
|----------|------|---------|
| `users` | 사용자 정보 | id, email, password, nickname, role, status |
| `diaries` | 일기 | id, userId, title, content, latitude, longitude, visibility |
| `diary_images` | 일기 이미지 | id, diaryId, imageUrl |
| `diary_shares` | 일기 공유 | id, diaryId, targetUserId |
| `scraps` | 일기 스크랩 | id, userId, diaryId |
| `friends` | 친구 관계 | id, requesterId, receiverId, status |
| `notifications` | 알림 | id, userId, type, referenceId, message, isRead |

---

## 테이블 상세 설명

### 1. users (사용자)

사용자 계정 정보를 저장합니다.

```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(50) NOT NULL UNIQUE,
    profile_image_url VARCHAR(1000),
    role ENUM('USER', 'ADMIN') NOT NULL DEFAULT 'USER',
    status ENUM('ACTIVE', 'SUSPENDED', 'DELETED') NOT NULL DEFAULT 'ACTIVE',
    suspension_reason VARCHAR(500),
    suspension_expires_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME,
    INDEX idx_email (email),
    INDEX idx_nickname (nickname),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**칼럼 설명**:

| 칼럼명 | 타입 | 설명 |
|--------|------|------|
| `id` | BIGINT | 기본 키 (AUTO_INCREMENT) |
| `email` | VARCHAR(255) | 사용자 이메일 (고유, 로그인 식별자) |
| `password` | VARCHAR(255) | 해싱된 비밀번호 (BCrypt) |
| `nickname` | VARCHAR(50) | 닉네임 (고유, 디스플레이) |
| `profile_image_url` | VARCHAR(1000) | 프로필 이미지 URL (S3 또는 로컬) |
| `role` | ENUM | 역할 (USER: 일반, ADMIN: 관리자) |
| `status` | ENUM | 계정 상태 (ACTIVE: 활성, SUSPENDED: 정지, DELETED: 삭제) |
| `suspension_reason` | VARCHAR(500) | 정지 사유 |
| `suspension_expires_at` | DATETIME | 정지 해제 예정일시 |
| `created_at` | DATETIME | 계정 생성일시 |
| `updated_at` | DATETIME | 최근 수정일시 |
| `deleted_at` | DATETIME | 소프트 삭제일시 |

**주요 로직**:
- 소프트 삭제: `deleted_at` 설정 (레코드 삭제 X)
- 비밀번호 암호화: Spring Security BCryptPasswordEncoder
- 이메일/닉네임: UNIQUE 제약

---

### 2. diaries (일기)

사용자의 일기를 저장합니다.

```sql
CREATE TABLE diaries (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    content LONGTEXT NOT NULL,
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL,
    location_name VARCHAR(255) NOT NULL,
    address VARCHAR(500),
    visited_at DATETIME NOT NULL,
    visibility ENUM('PUBLIC', 'PRIVATE', 'FRIENDS_ONLY') NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_visibility (visibility),
    INDEX idx_visited_at (visited_at),
    INDEX idx_latitude_longitude (latitude, longitude),
    INDEX idx_created_at (created_at),
    INDEX idx_deleted_at (deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**칼럼 설명**:

| 칼럼명 | 타입 | 설명 |
|--------|------|------|
| `id` | BIGINT | 기본 키 |
| `user_id` | BIGINT | 작성자 ID (FK -> users) |
| `title` | VARCHAR(255) | 일기 제목 |
| `content` | LONGTEXT | 일기 내용 |
| `latitude` | DOUBLE | 위도 (-90.0 ~ 90.0) |
| `longitude` | DOUBLE | 경도 (-180.0 ~ 180.0) |
| `location_name` | VARCHAR(255) | 위치명 (카카오맵 API) |
| `address` | VARCHAR(500) | 주소 |
| `visited_at` | DATETIME | 방문 날짜/시간 |
| `visibility` | ENUM | 공개 범위 (PUBLIC, PRIVATE, FRIENDS_ONLY) |
| `created_at` | DATETIME | 생성일시 |
| `updated_at` | DATETIME | 수정일시 |
| `deleted_at` | DATETIME | 소프트 삭제일시 |

**주요 로직**:
- 지도 조회 시 `latitude, longitude` 범위 검색
- 접근 제어: `visibility` 기반
  - PRIVATE: user_id 본인만
  - FRIENDS_ONLY: 본인 + 친구 관계인 사용자
  - PUBLIC: 모든 인증 사용자
- 소프트 삭제: `deleted_at` 설정

---

### 3. diary_images (일기 이미지)

일기에 첨부된 이미지를 저장합니다.

```sql
CREATE TABLE diary_images (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    diary_id BIGINT NOT NULL,
    image_url VARCHAR(1000) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (diary_id) REFERENCES diaries(id) ON DELETE CASCADE,
    INDEX idx_diary_id (diary_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**칼럼 설명**:

| 칼럼명 | 타입 | 설명 |
|--------|------|------|
| `id` | BIGINT | 기본 키 |
| `diary_id` | BIGINT | 일기 ID (FK -> diaries) |
| `image_url` | VARCHAR(1000) | 이미지 URL (S3/로컬 경로) |
| `created_at` | DATETIME | 생성일시 |

**주요 로직**:
- 일기와 일대다 관계
- 일기 삭제 시 자동 삭제 (CASCADE)
- 이미지 URL은 외부 스토리지 경로 (presigned URL 생성 가능)

---

### 4. diary_shares (일기 공유)

일기를 특정 사용자에게 공유하는 정보를 저장합니다.

```sql
CREATE TABLE diary_shares (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    diary_id BIGINT NOT NULL,
    target_user_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (diary_id) REFERENCES diaries(id) ON DELETE CASCADE,
    FOREIGN KEY (target_user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_diary_id (diary_id),
    INDEX idx_target_user_id (target_user_id),
    UNIQUE KEY uk_diary_target (diary_id, target_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**칼럼 설명**:

| 칼럼명 | 타입 | 설명 |
|--------|------|------|
| `id` | BIGINT | 기본 키 |
| `diary_id` | BIGINT | 일기 ID (FK -> diaries) |
| `target_user_id` | BIGINT | 공유 대상 사용자 ID (FK -> users) |
| `created_at` | DATETIME | 공유일시 |

**주요 로직**:
- 일기 작성 시 특정 사용자들에게 공유
- 공유 시 Notification 생성
- `(diary_id, target_user_id)` UNIQUE: 중복 공유 방지

---

### 5. scraps (스크랩)

사용자가 스크랩한 일기를 저장합니다.

```sql
CREATE TABLE scraps (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    diary_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (diary_id) REFERENCES diaries(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_diary_id (diary_id),
    UNIQUE KEY uk_user_diary (user_id, diary_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**칼럼 설명**:

| 칼럼명 | 타입 | 설명 |
|--------|------|------|
| `id` | BIGINT | 기본 키 |
| `user_id` | BIGINT | 스크랩한 사용자 ID (FK -> users) |
| `diary_id` | BIGINT | 스크랩된 일기 ID (FK -> diaries) |
| `created_at` | DATETIME | 스크랩일시 |

**주요 로직**:
- 사용자가 다른 사용자의 일기 보존
- `(user_id, diary_id)` UNIQUE: 중복 스크랩 방지
- 스크랩한 일기와 실제 일기 분리 (원본 일기 삭제해도 스크랩 기록 유지 가능)

---

### 6. friends (친구 관계)

사용자 간 친구 관계를 저장합니다.

```sql
CREATE TABLE friends (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    requester_id BIGINT NOT NULL,
    receiver_id BIGINT NOT NULL,
    status ENUM('PENDING', 'ACCEPTED', 'REJECTED') NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (requester_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (receiver_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_requester_id (requester_id),
    INDEX idx_receiver_id (receiver_id),
    INDEX idx_status (status),
    UNIQUE KEY uk_requester_receiver (requester_id, receiver_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**칼럼 설명**:

| 칼럼명 | 타입 | 설명 |
|--------|------|------|
| `id` | BIGINT | 기본 키 |
| `requester_id` | BIGINT | 요청 발송자 ID (FK -> users) |
| `receiver_id` | BIGINT | 요청 수신자 ID (FK -> users) |
| `status` | ENUM | 친구 상태 (PENDING: 보류, ACCEPTED: 수락, REJECTED: 거절) |
| `created_at` | DATETIME | 요청일시 |
| `updated_at` | DATETIME | 상태 변경일시 |

**주요 로직**:
- `(requester_id, receiver_id)` UNIQUE: 요청자-수신자 조합 중복 방지
- 상태 전이:
  - PENDING → ACCEPTED (수락)
  - PENDING → REJECTED (거절)
  - REJECTED → PENDING (재요청)
- 단방향 관계 (A→B와 B→A는 별개)

---

### 7. notifications (알림)

사용자에게 발생한 이벤트 알림을 저장합니다.

```sql
CREATE TABLE notifications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    type ENUM('FRIEND_REQUEST', 'FRIEND_ACCEPTED', 'DIARY_SHARED') NOT NULL,
    reference_id BIGINT,
    message VARCHAR(500) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_type (type),
    INDEX idx_is_read (is_read),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**칼럼 설명**:

| 칼럼명 | 타입 | 설명 |
|--------|------|------|
| `id` | BIGINT | 기본 키 |
| `user_id` | BIGINT | 알림 수신자 ID (FK -> users) |
| `type` | ENUM | 알림 종류 (FRIEND_REQUEST, FRIEND_ACCEPTED, DIARY_SHARED) |
| `reference_id` | BIGINT | 관련 엔티티 ID (Friend.id, Diary.id) |
| `message` | VARCHAR(500) | 알림 메시지 |
| `is_read` | BOOLEAN | 읽음 여부 |
| `created_at` | DATETIME | 생성일시 |
| `updated_at` | DATETIME | 수정일시 |

**주요 로직**:
- 친구 요청 발송 시: `FRIEND_REQUEST` 생성
- 친구 요청 수락 시: `FRIEND_ACCEPTED` 생성
- 일기 공유 시: `DIARY_SHARED` 생성
- SSE로 실시간 푸시
- `is_read` 플래그로 미읽음 알림 필터링

---

## 엔티티 관계도

### ER Diagram (Text)

```
┌──────────────┐
│    users     │
├──────────────┤
│ id (PK)      │
│ email        │
│ password     │
│ nickname     │
│ role         │
│ status       │
│ ...          │
└──────────────┘
       │
       ├─ 1:N ─→ diaries (user_id FK)
       ├─ 1:N ─→ diary_shares (target_user_id FK)
       ├─ 1:N ─→ scraps (user_id FK)
       ├─ 1:N ─→ friends (requester_id FK)
       ├─ 1:N ─→ friends (receiver_id FK)
       └─ 1:N ─→ notifications (user_id FK)

┌──────────────┐
│   diaries    │
├──────────────┤
│ id (PK)      │
│ user_id (FK) │
│ title        │
│ content      │
│ latitude     │
│ longitude    │
│ visibility   │
│ ...          │
└──────────────┘
       │
       ├─ 1:N ─→ diary_images (diary_id FK)
       ├─ 1:N ─→ diary_shares (diary_id FK)
       └─ 1:N ─→ scraps (diary_id FK)

┌──────────────────┐
│  diary_images    │
├──────────────────┤
│ id (PK)          │
│ diary_id (FK)    │
│ image_url        │
└──────────────────┘

┌──────────────────┐
│  diary_shares    │
├──────────────────┤
│ id (PK)          │
│ diary_id (FK)    │
│ target_user_id   │
└──────────────────┘

┌──────────────┐
│   scraps     │
├──────────────┤
│ id (PK)      │
│ user_id (FK) │
│ diary_id (FK)│
└──────────────┘

┌──────────────────┐
│    friends       │
├──────────────────┤
│ id (PK)          │
│ requester_id(FK) │
│ receiver_id(FK)  │
│ status           │
└──────────────────┘

┌──────────────────┐
│  notifications   │
├──────────────────┤
│ id (PK)          │
│ user_id (FK)     │
│ type             │
│ reference_id     │
│ message          │
│ is_read          │
└──────────────────┘
```

### 관계 요약

| 부모 엔티티 | 자식 엔티티 | 관계 | 설명 |
|-----------|-----------|------|------|
| User | Diary | 1:N | 한 사용자가 여러 일기 작성 |
| Diary | DiaryImage | 1:N | 한 일기가 여러 이미지 포함 |
| Diary | DiaryShare | 1:N | 한 일기를 여러 사용자에게 공유 |
| Diary | Scrap | 1:N | 한 일기를 여러 사용자가 스크랩 |
| User | Scrap | 1:N | 한 사용자가 여러 일기 스크랩 |
| User | Friend | 1:N (이중) | 한 사용자가 여러 요청 발송/수신 |
| User | Notification | 1:N | 한 사용자가 여러 알림 수신 |

---

## 인덱싱 전략

### 복합 인덱스 (Composite Index)

**diaries 테이블**:
```sql
-- 지도 조회: 위도, 경도 범위 검색
INDEX idx_latitude_longitude (latitude, longitude)

-- 사용자별 일기 조회
INDEX idx_user_id (user_id)
```

**friends 테이블**:
```sql
-- 특정 사용자의 친구 관계 빠르게 조회
INDEX idx_requester_id (requester_id)
INDEX idx_receiver_id (receiver_id)
INDEX idx_status (status)

-- 중복 요청 방지 (UNIQUE)
UNIQUE KEY uk_requester_receiver (requester_id, receiver_id)
```

**scraps 테이블**:
```sql
-- 중복 스크랩 방지 (UNIQUE)
UNIQUE KEY uk_user_diary (user_id, diary_id)
```

### 쿼리 최적화 포인트

1. **마커 조회** (지도):
   ```sql
   SELECT id, latitude, longitude, title FROM diaries
   WHERE latitude BETWEEN ? AND ?
     AND longitude BETWEEN ? AND ?
     AND deleted_at IS NULL
   ```
   → `idx_latitude_longitude` 인덱스 활용

2. **사용자별 일기 조회**:
   ```sql
   SELECT * FROM diaries
   WHERE user_id = ? AND deleted_at IS NULL
   ORDER BY created_at DESC
   ```
   → `idx_user_id` 인덱스 활용

3. **친구 관계 조회**:
   ```sql
   SELECT * FROM friends
   WHERE (requester_id = ? OR receiver_id = ?) AND status = 'ACCEPTED'
   ```
   → `idx_requester_id`, `idx_receiver_id`, `idx_status` 인덱스 활용

---

## 제약 조건

### UNIQUE 제약

```sql
-- users 테이블
UNIQUE KEY uk_email (email)
UNIQUE KEY uk_nickname (nickname)

-- diary_shares 테이블
UNIQUE KEY uk_diary_target (diary_id, target_user_id)

-- scraps 테이블
UNIQUE KEY uk_user_diary (user_id, diary_id)

-- friends 테이블
UNIQUE KEY uk_requester_receiver (requester_id, receiver_id)
```

### FOREIGN KEY 제약

모든 외래 키는 `ON DELETE CASCADE` 설정:

```sql
FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
```

**효과**:
- 사용자 삭제 시 관련 모든 일기, 친구 관계, 알림 등 자동 삭제
- 데이터 일관성 보장

### NOT NULL 제약

필수 필드:

```sql
-- users
id, email, password, nickname, role, status, created_at

-- diaries
id, user_id, title, content, latitude, longitude, location_name,
visited_at, visibility, created_at

-- diary_shares
id, diary_id, target_user_id, created_at

-- scraps
id, user_id, diary_id, created_at

-- friends
id, requester_id, receiver_id, status, created_at

-- notifications
id, user_id, type, message, is_read, created_at
```

---

## DDL 자동 생성

MapLog는 Hibernate `ddl-auto` 설정으로 자동 스키마 생성:

```yaml
# application-dev.yml (개발)
spring:
  jpa:
    hibernate:
      ddl-auto: update  # 스키마 자동 생성/수정

# application-aws.yml (운영)
spring:
  jpa:
    hibernate:
      ddl-auto: validate  # 기존 스키마와 엔티티 검증만
```

---

## 데이터 타입 선택 사유

| 필드 | 타입 | 사유 |
|------|------|------|
| id | BIGINT | 향후 확장성 (INT는 약 21억까지만) |
| email | VARCHAR(255) | RFC 5321 최대 254자 + 안여유 |
| password | VARCHAR(255) | BCrypt 해시는 60자 필요 |
| content | LONGTEXT | 일기 내용 길이 제한 없음 |
| latitude/longitude | DOUBLE | GPS 좌표 정밀도 |
| visibility | ENUM | 고정된 3가지 선택지 |
| role/status | ENUM | 고정된 선택지 (공간 효율) |

---

## 참고 자료

- [MariaDB 공식 문서](https://mariadb.com/docs/)
- [MySQL ENUM Type](https://dev.mysql.com/doc/refman/8.0/en/enum.html)
- [Hibernate Mapping](https://hibernate.org/orm/documentation/)
- [Database Normalization](https://en.wikipedia.org/wiki/Database_normalization)

---

**다음 문서 참고:**
- [아키텍처 개요](./01-architecture-overview.md)
- [API 설계 문서](./02-api-design.md)
- [개발 환경 설정](./04-development-setup.md)

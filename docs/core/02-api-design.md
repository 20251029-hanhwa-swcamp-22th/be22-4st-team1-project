# MapLog API 설계 문서

> **한 줄 요약**: MapLog는 RESTful API를 통해 회원 관리, 일기 CRUD, 친구 관계, 실시간 알림 등의 기능을 제공하며, 모든 응답은 통일된 ApiResponse 형식을 사용합니다.

## 목차 (Table of Contents)

- [개요](#개요)
- [응답 형식](#응답-형식)
- [인증 및 권한](#인증-및-권한)
- [API 엔드포인트](#api-엔드포인트)
- [오류 처리](#오류-처리)
- [페이지네이션](#페이지네이션)

---

## 개요

### Base URL

```
개발 환경: http://localhost:8080
프로덕션: https://maplog.example.com (또는 K8s Ingress URL)
```

### HTTP 메서드 규칙

| 메서드 | 용도 |
|--------|------|
| GET | 조회 (안전, 멱등성 보장) |
| POST | 생성 (201 Created) |
| PUT | 전체 수정 (200 OK) |
| DELETE | 삭제 (204 No Content) |

### Content-Type

```
요청: application/json (multipart/form-data for file upload)
응답: application/json
```

---

## 응답 형식

### 성공 응답

모든 성공 응답은 다음 형식을 따릅니다:

```json
{
  "code": "SUCCESS",
  "message": "요청이 성공했습니다.",
  "data": {
    // 실제 응답 데이터
  }
}
```

**응답 코드 필드 설명**:
- `code`: `"SUCCESS"` (항상 동일)
- `message`: 사용자 친화적 메시지
- `data`: 실제 응답 페이로드 (null 가능)

### 오류 응답

```json
{
  "code": "INVALID_TOKEN",
  "message": "유효하지 않은 토큰입니다.",
  "data": null
}
```

---

## 인증 및 권한

### JWT 토큰

**저장 위치**: `localStorage`

```javascript
localStorage.setItem('ml_access_token', 'eyJhbGciOiJIUzI1NiIs...')
localStorage.setItem('ml_refresh_token', 'eyJhbGciOiJIUzI1NiIs...')
```

**토큰 유효 기간**:
- Access Token: 30분 (1800000 ms)
- Refresh Token: 14일 (1209600000 ms)

### 인증 헤더

모든 인증이 필요한 요청에 다음 헤더 포함:

```
Authorization: Bearer {access_token}
```

**예시**:
```bash
curl -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIs..." \
  http://localhost:8080/api/users/me
```

### 공개 엔드포인트

인증 없이 접근 가능:

- `POST /api/auth/signup`
- `POST /api/auth/login`
- `POST /api/auth/refresh` (refresh token 필요)
- `GET /api/users/check-nickname`
- `GET /uploads/**` (정적 파일)

### 보호된 엔드포인트

JWT 토큰 필수:

- `/api/users/**` (본인 정보 조회/수정)
- `/api/diaries/**` (일기 CRUD)
- `/api/friends/**`
- `/api/feed`
- `/api/notifications/**`
- `/api/sse/**`

### 관리자 엔드포인트

`ADMIN` 역할 필수:

- `GET /api/admin/users`
- `PUT /api/admin/users/{id}/status`

---

## API 엔드포인트

### 1. Authentication (인증)

#### 회원가입

```http
POST /api/auth/signup
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "SecurePassword123!",
  "nickname": "john_doe"
}
```

**요청 검증**:
- `email`: 유효한 이메일 형식, 중복 불가
- `password`: 최소 8자
- `nickname`: 2~20자, 영문/한글/숫자/언더스코어, 중복 불가

**응답 (201 Created)**:
```json
{
  "code": "SUCCESS",
  "message": "회원가입이 완료되었습니다.",
  "data": null
}
```

**오류**:
- `400 BAD_REQUEST`: 검증 실패
- `409 CONFLICT`: `EMAIL_ALREADY_EXISTS` 또는 `NICKNAME_ALREADY_EXISTS`

---

#### 로그인

```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "SecurePassword123!"
}
```

**응답 (200 OK)**:
```json
{
  "code": "SUCCESS",
  "message": "로그인이 완료되었습니다.",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIs...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
  }
}
```

**오류**:
- `401 UNAUTHORIZED`: `INVALID_PASSWORD`
- `404 NOT_FOUND`: `USER_NOT_FOUND`

**클라이언트 처리**:
```javascript
const response = await authApi.login(credentials)
const { accessToken, refreshToken } = response.data
localStorage.setItem('ml_access_token', accessToken)
localStorage.setItem('ml_refresh_token', refreshToken)
```

---

#### 토큰 갱신

```http
POST /api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
}
```

**응답 (200 OK)**:
```json
{
  "code": "SUCCESS",
  "message": "요청이 성공했습니다.",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIs...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
  }
}
```

**오류**:
- `401 UNAUTHORIZED`: `EXPIRED_TOKEN` 또는 `INVALID_TOKEN`

---

#### 로그아웃

```http
POST /api/auth/logout
Authorization: Bearer {access_token}
```

**응답 (200 OK)**:
```json
{
  "code": "SUCCESS",
  "message": "로그아웃이 완료되었습니다.",
  "data": null
}
```

---

### 2. Users (사용자)

#### 내 정보 조회

```http
GET /api/users/me
Authorization: Bearer {access_token}
```

**응답 (200 OK)**:
```json
{
  "code": "SUCCESS",
  "message": "요청이 성공했습니다.",
  "data": {
    "id": 1,
    "email": "user@example.com",
    "nickname": "john_doe",
    "profileImageUrl": "https://s3.amazonaws.com/.../profile.jpg",
    "role": "USER",
    "status": "ACTIVE",
    "createdAt": "2025-02-20T10:30:00"
  }
}
```

---

#### 프로필 수정

```http
PUT /api/users/me
Authorization: Bearer {access_token}
Content-Type: multipart/form-data

{
  "nickname": "new_nickname",
  "profileImage": <file> (선택사항)
}
```

**응답 (200 OK)**:
```json
{
  "code": "SUCCESS",
  "message": "요청이 성공했습니다.",
  "data": {
    "id": 1,
    "nickname": "new_nickname",
    "profileImageUrl": "https://s3.amazonaws.com/.../new_profile.jpg"
  }
}
```

**오류**:
- `409 CONFLICT`: `NICKNAME_ALREADY_EXISTS`

---

#### 계정 삭제

```http
DELETE /api/users/me
Authorization: Bearer {access_token}
```

**응답 (204 No Content)**:
```
(응답 본문 없음)
```

---

#### 닉네임 중복 확인

```http
GET /api/users/check-nickname?nickname=john_doe
```

**응답 (200 OK)**:
```json
{
  "code": "SUCCESS",
  "message": "요청이 성공했습니다.",
  "data": {
    "available": true
  }
}
```

---

#### 사용자 검색

```http
GET /api/users/search?email=user@example.com
Authorization: Bearer {access_token}
```

**요청 파라미터**:
- `email` (query): 검색할 이메일 (부분 매칭)

**응답 (200 OK)**:
```json
{
  "code": "SUCCESS",
  "message": "요청이 성공했습니다.",
  "data": [
    {
      "id": 2,
      "email": "user@example.com",
      "nickname": "jane_doe",
      "profileImageUrl": "...",
      "createdAt": "2025-02-15T09:00:00"
    }
  ]
}
```

---

### 3. Diaries (일기)

#### 일기 작성

```http
POST /api/diaries
Authorization: Bearer {access_token}
Content-Type: multipart/form-data

{
  "title": "서울 명동 산책",
  "content": "오늘 날씨가 정말 좋았다...",
  "latitude": 37.563,
  "longitude": 126.986,
  "locationName": "명동거리",
  "address": "서울시 중구 명동",
  "visitedAt": "2025-02-20T14:30:00",
  "visibility": "PUBLIC",
  "images": [<file1>, <file2>],
  "sharedUserIds": [2, 3]
}
```

**요청 필드**:
- `title`: 일기 제목 (필수)
- `content`: 일기 내용 (필수, TEXT)
- `latitude`: 위도 (필수)
- `longitude`: 경도 (필수)
- `locationName`: 위치명 (필수)
- `address`: 주소 (선택)
- `visitedAt`: 방문 날짜/시간 (필수, ISO 8601)
- `visibility`: PUBLIC | PRIVATE | FRIENDS_ONLY (필수)
- `images`: 이미지 파일 배열 (선택, 최대 10MB/파일)
- `sharedUserIds`: 공유할 사용자 ID 배열 (선택)

**응답 (201 Created)**:
```json
{
  "code": "SUCCESS",
  "message": "요청이 성공했습니다.",
  "data": {
    "diaryId": 10
  }
}
```

---

#### 일기 상세 조회

```http
GET /api/diaries/10
Authorization: Bearer {access_token}
```

**응답 (200 OK)**:
```json
{
  "code": "SUCCESS",
  "message": "요청이 성공했습니다.",
  "data": {
    "id": 10,
    "userId": 1,
    "title": "서울 명동 산책",
    "content": "오늘 날씨가 정말 좋았다...",
    "latitude": 37.563,
    "longitude": 126.986,
    "locationName": "명동거리",
    "address": "서울시 중구 명동",
    "visitedAt": "2025-02-20T14:30:00",
    "visibility": "PUBLIC",
    "images": [
      {
        "id": 1,
        "imageUrl": "https://s3.amazonaws.com/.../img1.jpg"
      },
      {
        "id": 2,
        "imageUrl": "https://s3.amazonaws.com/.../img2.jpg"
      }
    ],
    "author": {
      "id": 1,
      "nickname": "john_doe",
      "profileImageUrl": "..."
    },
    "isOwner": true,
    "isScrapped": false,
    "createdAt": "2025-02-20T15:00:00",
    "updatedAt": "2025-02-20T15:00:00"
  }
}
```

**오류**:
- `403 FORBIDDEN`: `DIARY_ACCESS_DENIED` (접근 권한 없음)
- `404 NOT_FOUND`: `DIARY_NOT_FOUND`

---

#### 일기 수정

```http
PUT /api/diaries/10
Authorization: Bearer {access_token}
Content-Type: multipart/form-data

{
  "title": "서울 명동 산책 (수정)",
  "content": "날씨가 좋았던 하루...",
  "visitedAt": "2025-02-20T14:30:00",
  "visibility": "FRIENDS_ONLY",
  "deleteImageIds": [1],
  "images": [<new_file>]
}
```

**응답 (200 OK)**:
```json
{
  "code": "SUCCESS",
  "message": "요청이 성공했습니다.",
  "data": null
}
```

---

#### 일기 삭제

```http
DELETE /api/diaries/10
Authorization: Bearer {access_token}
```

**응답 (204 No Content)**:
```
(응답 본문 없음)
```

---

#### 지도 마커 조회

```http
GET /api/diaries/markers?minLat=37.4&maxLat=37.7&minLng=126.8&maxLng=127.2
Authorization: Bearer {access_token}
```

**요청 파라미터**:
- `minLat` (query): 최소 위도
- `maxLat` (query): 최대 위도
- `minLng` (query): 최소 경도
- `maxLng` (query): 최대 경도

**응답 (200 OK)**:
```json
{
  "code": "SUCCESS",
  "message": "요청이 성공했습니다.",
  "data": [
    {
      "diaryId": 10,
      "latitude": 37.563,
      "longitude": 126.986,
      "title": "서울 명동 산책",
      "visibility": "PUBLIC"
    },
    {
      "diaryId": 11,
      "latitude": 37.498,
      "longitude": 126.889,
      "title": "한강 공원",
      "visibility": "FRIENDS_ONLY"
    }
  ]
}
```

---

#### 내 일기 목록

```http
GET /api/diaries?page=0&size=20&sort=createdAt,desc
Authorization: Bearer {access_token}
```

**요청 파라미터** (페이지네이션):
- `page` (query): 페이지 번호 (0부터 시작)
- `size` (query): 한 페이지 크기 (기본값: 20)
- `sort` (query): 정렬 (필드,방향)

**응답 (200 OK)**:
```json
{
  "code": "SUCCESS",
  "message": "요청이 성공했습니다.",
  "data": {
    "content": [
      {
        "id": 10,
        "title": "서울 명동 산책",
        "locationName": "명동거리",
        "thumbnailImage": "https://...",
        "createdAt": "2025-02-20T15:00:00",
        "visibility": "PUBLIC"
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 20,
      "sort": {
        "empty": false,
        "sorted": true,
        "unsorted": false
      }
    },
    "totalElements": 45,
    "totalPages": 3,
    "first": true,
    "last": false,
    "empty": false
  }
}
```

---

### 4. Scraps (스크랩)

#### 스크랩 추가

```http
POST /api/diaries/10/scraps
Authorization: Bearer {access_token}
```

**응답 (201 Created)**:
```json
{
  "code": "SUCCESS",
  "message": "요청이 성공했습니다.",
  "data": {
    "scrapId": 5
  }
}
```

**오류**:
- `409 CONFLICT`: `ALREADY_SCRAPED`

---

#### 스크랩 삭제

```http
DELETE /api/diaries/10/scraps
Authorization: Bearer {access_token}
```

**응답 (204 No Content)**:
```
(응답 본문 없음)
```

**오류**:
- `404 NOT_FOUND`: `SCRAP_NOT_FOUND`

---

#### 내 스크랩 목록

```http
GET /api/diaries/scraps?page=0&size=20
Authorization: Bearer {access_token}
```

**응답 (200 OK)**:
```json
{
  "code": "SUCCESS",
  "message": "요청이 성공했습니다.",
  "data": {
    "content": [
      {
        "id": 10,
        "title": "서울 명동 산책",
        "locationName": "명동거리",
        "author": {
          "id": 2,
          "nickname": "jane_doe"
        },
        "createdAt": "2025-02-19T10:00:00"
      }
    ],
    "pageable": { ... },
    "totalElements": 15,
    "totalPages": 1,
    "first": true,
    "last": true,
    "empty": false
  }
}
```

---

### 5. Friends (친구)

#### 친구 요청 발송

```http
POST /api/friends/requests
Authorization: Bearer {access_token}
Content-Type: application/json

{
  "receiverId": 2
}
```

**응답 (201 Created)**:
```json
{
  "code": "SUCCESS",
  "message": "요청이 성공했습니다.",
  "data": {
    "friendRequestId": 7
  }
}
```

**오류**:
- `400 BAD_REQUEST`: `FRIEND_REQUEST_SELF`
- `409 CONFLICT`: `ALREADY_FRIEND` 또는 `ALREADY_FRIEND_REQUESTED`

---

#### 친구 요청 수락

```http
POST /api/friends/requests/7/accept
Authorization: Bearer {access_token}
```

**응답 (200 OK)**:
```json
{
  "code": "SUCCESS",
  "message": "요청이 성공했습니다.",
  "data": null
}
```

---

#### 친구 요청 거절

```http
POST /api/friends/requests/7/reject
Authorization: Bearer {access_token}
```

**응답 (200 OK)**:
```json
{
  "code": "SUCCESS",
  "message": "요청이 성공했습니다.",
  "data": null
}
```

---

#### 친구 삭제

```http
DELETE /api/friends/2
Authorization: Bearer {access_token}
```

**응답 (204 No Content)**:
```
(응답 본문 없음)
```

---

#### 친구 목록

```http
GET /api/friends?page=0&size=20
Authorization: Bearer {access_token}
```

**응답 (200 OK)**:
```json
{
  "code": "SUCCESS",
  "message": "요청이 성공했습니다.",
  "data": {
    "content": [
      {
        "id": 2,
        "email": "jane@example.com",
        "nickname": "jane_doe",
        "profileImageUrl": "...",
        "status": "ACCEPTED"
      }
    ],
    "pageable": { ... },
    "totalElements": 5,
    "totalPages": 1,
    "first": true,
    "last": true,
    "empty": false
  }
}
```

---

#### 보류 중인 친구 요청

```http
GET /api/friends/pending?page=0&size=20
Authorization: Bearer {access_token}
```

**응답 (200 OK)**:
```json
{
  "code": "SUCCESS",
  "message": "요청이 성공했습니다.",
  "data": {
    "content": [
      {
        "id": 7,
        "requester": {
          "id": 3,
          "nickname": "bob_smith",
          "profileImageUrl": "..."
        },
        "status": "PENDING",
        "createdAt": "2025-02-20T10:00:00"
      }
    ],
    "pageable": { ... },
    "totalElements": 2,
    "totalPages": 1,
    "first": true,
    "last": true,
    "empty": false
  }
}
```

---

### 6. Feed (피드)

#### 친구 피드 조회

```http
GET /api/feed?page=0&size=20&sort=createdAt,desc
Authorization: Bearer {access_token}
```

**응답 (200 OK)**:
```json
{
  "code": "SUCCESS",
  "message": "요청이 성공했습니다.",
  "data": {
    "content": [
      {
        "id": 10,
        "title": "서울 명동 산책",
        "locationName": "명동거리",
        "author": {
          "id": 2,
          "nickname": "jane_doe",
          "profileImageUrl": "..."
        },
        "thumbnailImage": "https://...",
        "visibility": "FRIENDS_ONLY",
        "createdAt": "2025-02-20T15:00:00"
      }
    ],
    "pageable": { ... },
    "totalElements": 30,
    "totalPages": 2,
    "first": true,
    "last": false,
    "empty": false
  }
}
```

---

### 7. Notifications (알림)

#### 알림 목록 조회

```http
GET /api/notifications?readFilter=UNREAD&page=0&size=20
Authorization: Bearer {access_token}
```

**요청 파라미터**:
- `readFilter` (query): UNREAD | READ | ALL (기본값: ALL)

**응답 (200 OK)**:
```json
{
  "code": "SUCCESS",
  "message": "요청이 성공했습니다.",
  "data": {
    "content": [
      {
        "id": 5,
        "type": "FRIEND_REQUEST",
        "message": "'jane_doe'님으로부터 새로운 친구 요청이 도착했습니다.",
        "referenceId": 2,
        "isRead": false,
        "createdAt": "2025-02-20T14:30:00"
      },
      {
        "id": 4,
        "type": "DIARY_SHARED",
        "message": "'bob_smith'님이 '한강 산책' 일기를 공유했습니다.",
        "referenceId": 15,
        "isRead": true,
        "createdAt": "2025-02-20T12:00:00"
      }
    ],
    "pageable": { ... },
    "totalElements": 8,
    "totalPages": 1,
    "first": true,
    "last": true,
    "empty": false
  }
}
```

---

#### 알림 읽음 표시

```http
POST /api/notifications/5/read
Authorization: Bearer {access_token}
```

**응답 (200 OK)**:
```json
{
  "code": "SUCCESS",
  "message": "요청이 성공했습니다.",
  "data": null
}
```

---

#### 모든 알림 읽음 처리

```http
POST /api/notifications/read-all
Authorization: Bearer {access_token}
```

**응답 (200 OK)**:
```json
{
  "code": "SUCCESS",
  "message": "요청이 성공했습니다.",
  "data": null
}
```

---

#### 알림 삭제

```http
DELETE /api/notifications/5
Authorization: Bearer {access_token}
```

**응답 (204 No Content)**:
```
(응답 본문 없음)
```

---

#### 알림 일괄 삭제

```http
DELETE /api/notifications?readFilter=READ
Authorization: Bearer {access_token}
```

**요청 파라미터**:
- `readFilter` (query): UNREAD | READ | ALL

**응답 (204 No Content)**:
```
(응답 본문 없음)
```

---

### 8. Server-Sent Events (SSE)

#### SSE 구독

```http
GET /api/sse/subscribe?token={access_token}
Authorization: (불가능, 헤더 미지원 - 쿼리 파라미터 사용)

HTTP/1.1 200 OK
Content-Type: text/event-stream
Cache-Control: no-cache
Connection: keep-alive

event: notification
data: {"type":"FRIEND_REQUEST","message":"'jane_doe'님으로부터 새로운 친구 요청이 도착했습니다."}

event: notification
data: {"type":"FRIEND_ACCEPTED","message":"'jane_doe'님이 친구 요청을 수락했습니다."}

event: notification
data: {"type":"DIARY_SHARED","message":"'bob_smith'님이 '한강 산책' 일기를 공유했습니다."}
```

**프론트엔드 구현**:
```javascript
const eventSource = new EventSource(`/api/sse/subscribe?token=${accessToken}`)

eventSource.addEventListener('notification', (event) => {
  const data = JSON.parse(event.data)
  console.log(data.type, data.message)
  // 알림 처리 로직
})

eventSource.onerror = () => {
  eventSource.close()
  // 재연결 로직
}
```

---

### 9. Admin (관리자)

#### 전체 사용자 목록

```http
GET /api/admin/users?page=0&size=20&status=ALL
Authorization: Bearer {admin_access_token}
```

**요청 파라미터**:
- `status` (query): ACTIVE | SUSPENDED | ALL

**응답 (200 OK)**:
```json
{
  "code": "SUCCESS",
  "message": "요청이 성공했습니다.",
  "data": {
    "content": [
      {
        "id": 1,
        "email": "user@example.com",
        "nickname": "john_doe",
        "status": "ACTIVE",
        "role": "USER",
        "createdAt": "2025-02-15T08:00:00"
      }
    ],
    "pageable": { ... },
    "totalElements": 100,
    "totalPages": 5,
    "first": true,
    "last": false,
    "empty": false
  }
}
```

---

#### 사용자 상태 변경

```http
PUT /api/admin/users/5/status
Authorization: Bearer {admin_access_token}
Content-Type: application/json

{
  "status": "SUSPENDED",
  "suspensionReason": "규칙 위반",
  "suspensionExpiresAt": "2025-03-20T00:00:00"
}
```

**요청 필드**:
- `status`: ACTIVE | SUSPENDED
- `suspensionReason` (조건부): status가 SUSPENDED일 경우 필수
- `suspensionExpiresAt` (선택): 정지 해제 날짜

**응답 (200 OK)**:
```json
{
  "code": "SUCCESS",
  "message": "요청이 성공했습니다.",
  "data": null
}
```

---

## 오류 처리

### ErrorCode 전체 목록

| ErrorCode | HTTP 상태 | 메시지 |
|-----------|----------|--------|
| `BAD_REQUEST` | 400 | 잘못된 요청입니다. |
| `UNAUTHORIZED` | 401 | 인증이 필요합니다. |
| `FORBIDDEN` | 403 | 접근 권한이 없습니다. |
| `NOT_FOUND` | 404 | 요청한 리소스를 찾을 수 없습니다. |
| `INTERNAL_SERVER_ERROR` | 500 | 서버 내부 오류가 발생했습니다. |
| `INVALID_TOKEN` | 401 | 유효하지 않은 토큰입니다. |
| `EXPIRED_TOKEN` | 401 | 만료된 토큰입니다. |
| `USER_NOT_FOUND` | 404 | 존재하지 않는 사용자입니다. |
| `EMAIL_ALREADY_EXISTS` | 409 | 이미 사용 중인 이메일입니다. |
| `NICKNAME_ALREADY_EXISTS` | 409 | 이미 사용 중인 닉네임입니다. |
| `INVALID_PASSWORD` | 401 | 비밀번호가 올바르지 않습니다. |
| `DIARY_NOT_FOUND` | 404 | 존재하지 않는 일기입니다. |
| `DIARY_ACCESS_DENIED` | 403 | 일기에 접근 권한이 없습니다. |
| `ALREADY_SCRAPED` | 409 | 이미 스크랩한 일기입니다. |
| `SCRAP_NOT_FOUND` | 404 | 스크랩을 찾을 수 없습니다. |
| `FRIEND_REQUEST_NOT_FOUND` | 404 | 친구 요청을 찾을 수 없습니다. |
| `ALREADY_FRIEND` | 409 | 이미 친구 관계입니다. |
| `ALREADY_FRIEND_REQUESTED` | 409 | 이미 친구 요청이 진행 중입니다. |
| `FRIEND_REQUEST_SELF` | 400 | 자기 자신에게 친구 요청을 보낼 수 없습니다. |
| `NOTIFICATION_NOT_FOUND` | 404 | 존재하지 않는 알림입니다. |
| `INVALID_FILE` | 400 | 유효하지 않은 파일입니다. |
| `FILE_UPLOAD_FAILED` | 500 | 파일 업로드에 실패했습니다. |
| `DIARY_SHARE_NOT_FOUND` | 404 | 공유 정보를 찾을 수 없습니다. |

### 예시: 오류 응답

```json
{
  "code": "INVALID_TOKEN",
  "message": "유효하지 않은 토큰입니다.",
  "data": null
}
```

**HTTP 상태 코드**는 응답 헤더에 포함됩니다:
```
HTTP/1.1 401 Unauthorized
Content-Type: application/json
```

---

## 페이지네이션

### 형식

Spring Data의 `Page<T>` 응답 형식:

```json
{
  "code": "SUCCESS",
  "message": "요청이 성공했습니다.",
  "data": {
    "content": [...],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 20,
      "sort": {
        "empty": false,
        "sorted": true,
        "unsorted": false
      }
    },
    "totalElements": 100,
    "totalPages": 5,
    "first": true,
    "last": false,
    "empty": false,
    "numberOfElements": 20
  }
}
```

### 요청 파라미터

```
GET /api/diaries?page=0&size=20&sort=createdAt,desc&sort=id,asc
```

- `page`: 페이지 번호 (0부터 시작, 기본값: 0)
- `size`: 한 페이지 항목 수 (기본값: 20)
- `sort`: 정렬 기준 (필드,방향), 복수 가능

### 정렬 방향

```
sort=createdAt,desc      # 생성일 기준 내림차순
sort=createdAt,asc       # 생성일 기준 오름차순
```

---

## 참고 자료

- [RESTful API 설계 가이드](https://restfulapi.net/)
- [RFC 7231 - HTTP Method Definitions](https://tools.ietf.org/html/rfc7231#section-4)
- [OAuth 2.0 Bearer Token](https://tools.ietf.org/html/rfc6750)
- [Server-Sent Events](https://html.spec.whatwg.org/multipage/server-sent-events.html)

---

**다음 문서 참고:**
- [아키텍처 개요](./01-architecture-overview.md)
- [데이터베이스 스키마](./03-database-schema.md)
- [인증/보안 설계](./06-auth-security.md)

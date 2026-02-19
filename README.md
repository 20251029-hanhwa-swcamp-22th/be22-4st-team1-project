# 📍 MapLog — 지도 위의 나만의 일기

> 방문한 장소를 지도에 마킹하고 일기를 기록하는 소셜 다이어리 서비스

---

## 📌 목차

- [프로젝트 소개](#프로젝트-소개)
- [기술 스택](#기술-스택)
- [아키텍처](#아키텍처)
- [패키지 구조](#패키지-구조)
- [프론트엔드 구조](#프론트엔드-구조)
- [역할 분담](#역할-분담)
- [프론트엔드 개발 가이드](#프론트엔드-개발-가이드)
- [개발 환경 설정](#개발-환경-설정)
- [환경 변수](#환경-변수)
- [API 공통 규격](#api-공통-규격)
- [주요 API 엔드포인트](#주요-api-엔드포인트)
- [테스트](#테스트)
- [Git 컨벤션](#git-컨벤션)
- [기여 가이드](#기여-가이드)
- [라이선스](#라이선스)

---

## 프로젝트 소개

MapLog는 사용자가 방문한 장소를 지도에 마킹하고, 그 위에 일기를 작성할 수 있는 소셜 다이어리 서비스입니다.

### ✨ 핵심 기능

| 기능 | 설명 |
|------|------|
| 🗺️ 지도 기반 일기 | 위치 정보와 함께 일기 작성, 지도 위에 마커로 표시 |
| 🔒 공개 범위 설정 | 전체 공개 / 친구 공개 / 비공개 선택 |
| 👥 소셜 기능 | 친구 추가, 친구 피드 조회 |
| 🔖 스크랩 | 마음에 드는 일기 북마크 |
| 🔔 알림 | 친구 요청 등 알림 |

---

## 기술 스택

### Backend

| 분류 | 기술 |
|------|------|
| Language | Java 21 |
| Framework | Spring Boot 3.5 |
| Build | Gradle |
| ORM | Spring Data JPA |
| DB | MariaDB 11 |
| Security | Spring Security + JWT |
| Infra | Docker, Kubernetes, Jenkins |

### Frontend

| 분류 | 기술 |
|------|------|
| Framework | Vue 3 (Composition API) |
| Router | Vue Router 4 |
| Build | Vite |
| HTTP | Axios |

---

## 아키텍처

```
┌────────────────────────────────────────────────┐
│                   Vue 3 Frontend               │
└───────────────────────┬────────────────────────┘
                        │ REST API
┌───────────────────────▼────────────────────────┐
│              Spring Boot Backend               │
│                                                │
│  ┌─────────────────────┐  ┌──────────────────┐ │
│  │    Spring Data JPA  │  │  Spring Security │ │
│  │      (ORM)          │  │   JWT Auth       │ │
│  └──────────┬──────────┘  └──────────────────┘ │
│             │                                  │
│  ┌──────────▼──────────────────────────────┐   │
│  │             MariaDB 11                   │   │
│  └─────────────────────────────────────────┘   │
└────────────────────────────────────────────────┘
```

---

## 패키지 구조

```
src/main/java/com/maplog/
├── user/             # 회원 + 인증 (JWT, 로그인/회원가입)
│   ├── command/      # 쓰기 (Controller, Service, Domain, Repository, DTO)
│   └── query/        # 읽기 (Controller, Service, Repository, DTO)
├── diary/            # 일기 + 스크랩
│   ├── command/
│   └── query/
├── friend/           # 친구
│   ├── command/
│   └── query/
├── notification/     # 알림
│   ├── command/
│   └── query/
└── common/           # 공통 (응답 포맷, 예외처리, JWT 필터, 설정)
```

---

## 프론트엔드 구조

```
map-log-frontend/
├── index.html
├── vite.config.js
├── package.json
└── src/
    ├── main.js
    └── app/
        ├── App.vue
        ├── router/
        │   └── index.js
        ├── api/
        │   ├── axios.js
        │   └── diary.js
        ├── components/
        │   └── Layout.vue
        ├── styles/
        │   └── index.css
        ├── data/
        │   └── MockData.js
        └── views/
            ├── LoginView.vue
            ├── SignUpView.vue
            ├── MapView.vue
            ├── FeedView.vue
            ├── FriendView.vue
            ├── NotificationsView.vue
            ├── MyPageView.vue
            ├── DiaryDetailView.vue
            └── AdminView.vue
```

### 화면/라우트 설계(초안)

| 화면(View) | Path(예시) | 설명 |
|------|------|------|
| `LoginView` | `/login` | 로그인 |
| `SignUpView` | `/signup` | 회원가입 |
| `MapView` | `/map` | 지도 기반 일기 목록/작성 진입 |
| `DiaryDetailView` | `/diaries/:diaryId` | 일기 상세 |
| `FeedView` | `/feed` | 친구 피드 |
| `FriendView` | `/friends` | 친구 목록/요청 |
| `NotificationsView` | `/notifications` | 알림 목록 |
| `MyPageView` | `/mypage` | 마이페이지 |
| `AdminView` | `/admin` | 관리자 화면 |

### 프론트 데이터 흐름(권장)

1. View에서 사용자 액션 발생
2. `src/app/api/*` 모듈에서 백엔드 API 호출
3. `ApiResponse` 포맷 파싱 후 화면 상태 업데이트
4. 인증 필요 API는 `Authorization: Bearer {token}` 헤더 사용

> 현재 프론트 파일은 스켈레톤 상태이며, 라우터/앱 진입점/axios 모듈 구현이 필요한 단계입니다.

---

## 역할 분담

| 담당 | 도메인 | 화면 |
|------|------|------|
| **A** | `user` (회원 + 인증/JWT + 관리자 API) | LoginView, SignUpView, MyPageView |
| **B** | `diary` (일기 + 스크랩) | MapView, DiaryWriteView, DiaryDetailView |
| **C** | `friend` + `notification` | FeedView, FriendsView, NotificationsView |
| **D** | `common` (공통 기반 세팅) | — |

### D 상세 작업 목록 (1일차 최우선 완료)

| 순서 | 작업 | 설명 |
|------|------|------|
| 1 | `build.gradle` 의존성 추가 | Web, JPA, Security, MySQL, JWT, Validation 등 |
| 2 | `application-dev.yml` DB 설정 | 로컬 MySQL 연결 확인 |
| 3 | `ApiResponse<T>` 완성 | 공통 응답 포맷, 에러 코드 enum 정의 |
| 4 | `GlobalExceptionHandler` 구현 | `@RestControllerAdvice` 전역 예외 처리 |
| 5 | `AppConfig` 구현 | CORS 설정, PasswordEncoder Bean 등록 |
| 6 | `SecurityConfig` 구현 | JWT 필터 체인, 인증 불필요 경로 설정 |
| 7 | `JwtTokenProvider` 구현 | 토큰 생성/검증/파싱 유틸 (A와 협업) |
| 8 | DB 테이블 설계 확정 | 전체 팀 ERD 리뷰 및 DDL 공유 |

> D가 1~7을 완료해야 A·B·C가 본격적으로 시작 가능합니다.

### 개발 시작 순서

```
D → common 공통 기반 세팅 (최우선)
         ↓
    A → User Entity + JWT 완성
         ↓              ↓
    B (일기)        C (소셜)
```

---

## 프론트엔드 개발 가이드

### 로컬 실행

```bash
cd map-log-frontend
npm install
npm run dev
```

### 프로덕션 빌드

```bash
cd map-log-frontend
npm run build
```

### 환경 변수 (`.env.local`)

```bash
VITE_API_BASE_URL=http://localhost:8080
```

### API 연동 규칙

1. 공통 HTTP 클라이언트는 `src/app/api/axios.js`에서 생성/관리
2. 도메인 API는 `src/app/api/diary.js`처럼 기능별 파일로 분리
3. 응답은 백엔드 공통 포맷(`code`, `message`, `data`) 기준으로 처리
4. 401/403 등 인증 오류는 인터셉터에서 공통 처리

---

## 개발 환경 설정

### 사전 준비

- Java 21
- Docker Desktop
- Node.js 20+

### Backend 실행

```bash
# 1. 저장소 클론
git clone https://github.com/{org}/map-log.git
cd map-log

# 2. MariaDB 컨테이너 실행 (최초 1회)
docker compose up -d

# 3. 백엔드 실행
cd map-log-backend
./gradlew bootRun
```

> 기본 프로필은 `dev`입니다. 운영 환경은 `--spring.profiles.active=prod`로 실행합니다.

### MariaDB 컨테이너 관리

```bash
docker compose up -d    # 시작
docker compose down     # 중지 (데이터 유지)
docker compose down -v  # 중지 + 데이터 삭제
```

### Frontend 실행

```bash
cd map-log-frontend
npm install
npm run dev
```

> 브라우저에서 `http://localhost:5173` 접속

---

## 환경 변수

Spring Boot는 `application-{profile}.yml`로 환경별 설정을 관리합니다.

### 개발 환경 (`application-dev.yml`)

로컬 개발 시 아래 값을 본인 환경에 맞게 수정하세요. **이 파일은 커밋해도 됩니다.**

```yaml
spring:
  datasource:
    url: jdbc:mariadb://localhost:3306/maplog_dev
    username: root
    password: 1234   # 본인 MariaDB 비밀번호로 변경

jwt:
  secret: dev-secret-key-must-be-at-least-32-characters-long
```

### 운영 환경 (`application-prod.yml`)

`${변수명}` 형태로 작성되어 있으며, 실제 값은 서버 환경 변수 또는 Docker/Kubernetes 시크릿으로 주입합니다. **절대 실제 값을 yml에 직접 작성하지 마세요.**

| 변수명 | 설명 |
|--------|------|
| `DB_HOST` | MySQL 호스트 (RDS 엔드포인트 등) |
| `DB_NAME` | 데이터베이스명 |
| `DB_USERNAME` | DB 사용자 |
| `DB_PASSWORD` | DB 비밀번호 |
| `JWT_SECRET` | JWT 서명 키 (32자 이상) |

---

## API 공통 규격

### 응답 포맷

```json
{
  "code": "SUCCESS",
  "message": "요청이 성공했습니다.",
  "data": { }
}
```

### 에러 응답 포맷

```json
{
  "code": "USER_NOT_FOUND",
  "message": "존재하지 않는 사용자입니다.",
  "data": null
}
```

### 페이징

```json
{
  "code": "SUCCESS",
  "message": "조회 성공",
  "data": {
    "content": [],
    "page": 0,
    "size": 10,
    "totalElements": 100,
    "totalPages": 10,
    "last": false
  }
}
```

### 인증 헤더

```
Authorization: Bearer {accessToken}
```

### 날짜 형식

```
yyyy-MM-ddTHH:mm:ss  (ISO 8601)
예) 2026-02-19T14:30:00
```

---

## 주요 API 엔드포인트

### 사용자 / 인증 `담당: A`

| No | Method | Endpoint | 설명 | 인증 | 우선순위 |
|----|--------|----------|------|------|---------|
| 1 | POST | `/api/auth/signup` | 회원가입 | 불필요 | 필수 |
| 2 | POST | `/api/auth/login` | 로그인 | 불필요 | 필수 |
| 3 | POST | `/api/auth/refresh` | 토큰 재발급 | RT | 필수 |
| 4 | POST | `/api/auth/logout` | 로그아웃 | 필요 | 필수 |
| 5 | GET | `/api/users/me` | 마이페이지 조회 | 필요 | 필수 |
| 6 | PATCH | `/api/users/me` | 프로필 수정 | 필요 | 필수 |
| 7 | GET | `/api/users/me/diaries` | 내 일기 목록 | 필요 | 필수 |
| 8 | GET | `/api/users/me/scraps` | 내 스크랩 목록 | 필요 | 보통 |
| 9 | DELETE | `/api/users/me` | 회원 탈퇴 | 필요 | 중요 |
| 10 | GET | `/api/users/search` | 사용자 검색 | 필요 | 보통 |
| 11 | GET | `/api/admin/users` | 회원 목록 조회 | ADMIN | 중요 |
| 12 | PATCH | `/api/admin/users/{userId}/status` | 회원 상태 변경 (정지/활성화) | ADMIN | 중요 |

> No.11, 12는 별도 `admin` 도메인 없이 `user` 도메인 내 `AdminUserController`로 구현

### 일기 / 스크랩 `담당: B`

| No | Method | Endpoint | 설명 | 인증 | 우선순위 |
|----|--------|----------|------|------|---------|
| 13 | POST | `/api/diaries` | 일기 작성 | 필요 | 필수 |
| 14 | GET | `/api/diaries/{diaryId}` | 일기 상세 조회 | 필요 | 필수 |
| 15 | PUT | `/api/diaries/{diaryId}` | 일기 수정 | 필요 | 필수 |
| 16 | DELETE | `/api/diaries/{diaryId}` | 일기 삭제 (Soft Delete) | 필요 | 필수 |
| 17 | GET | `/api/diaries/map` | 지도 범위 내 마커 조회 | 필요 | 필수 |
| 18 | POST | `/api/scraps` | 스크랩 추가 | 필요 | 보통 |
| 19 | DELETE | `/api/scraps/{diaryId}` | 스크랩 취소 | 필요 | 보통 |

### 친구 / 알림 `담당: C`

| No | Method | Endpoint | 설명 | 인증 | 우선순위 |
|----|--------|----------|------|------|---------|
| 20 | POST | `/api/friends` | 친구 요청 | 필요 | 보통 |
| 21 | PATCH | `/api/friends/{friendId}` | 친구 요청 응답 (수락/거절) | 필요 | 보통 |
| 22 | GET | `/api/friends` | 친구 목록 조회 | 필요 | 보통 |
| 23 | GET | `/api/friends/pending` | 받은 친구 요청 목록 | 필요 | 보통 |
| 24 | GET | `/api/feed` | 친구 공개 일기 피드 | 필요 | 보통 |
| 25 | GET | `/api/notifications` | 알림 목록 조회 | 필요 | 중요 |
| 26 | PATCH | `/api/notifications/{notificationId}/read` | 알림 단건 읽음 | 필요 | 중요 |
| 27 | PATCH | `/api/notifications/read-all` | 알림 전체 읽음 | 필요 | 중요 |

#### 요청 예시

```json
POST /api/diaries
Authorization: Bearer {accessToken}

{
  "title": "서울 여행",
  "content": "오늘 경복궁을 다녀왔다.",
  "latitude": 37.5796,
  "longitude": 126.9770,
  "locationName": "경복궁",
  "address": "서울특별시 종로구 사직로 161",
  "visitedAt": "2026-02-19T14:00:00"
}
```

#### 응답 예시

```json
{
  "code": "SUCCESS",
  "message": "일기가 작성되었습니다.",
  "data": {
    "diaryId": 1,
    "title": "서울 여행",
    "createdAt": "2026-02-19T14:30:00"
  }
}
```

---

## 테스트

```bash
cd map-log-backend

# 전체 테스트 실행
./gradlew test

# 특정 도메인 테스트만 실행
./gradlew test --tests "com.maplog.diary.*"
```

> 테스트 결과는 `build/reports/tests/test/index.html` 에서 확인할 수 있습니다.

---

## Git 컨벤션

### 브랜치 전략

```
main
└── dev
    ├── feature/A-user
    ├── feature/B-diary
    ├── feature/C-social
    └── feature/D-common
```

### 커밋 메시지

```
feat:     새로운 기능
fix:      버그 수정
refactor: 리팩토링
test:     테스트 코드
docs:     문서 수정
chore:    빌드/설정 변경
```

예시

```
feat: 일기 작성 API 구현
fix: JWT 토큰 만료 처리 오류 수정
```

### PR 규칙

- `feature/*` → `dev` 로 PR
- 최소 1명 이상 코드 리뷰 후 머지
- `main` 머지는 배포 시에만

---

## 기여 가이드

1. `dev` 브랜치에서 본인 담당 `feature/*` 브랜치 생성
2. 작업 완료 후 `dev`로 PR 생성
3. PR 제목에 담당 파트 명시 (예: `[B] 일기 작성 API 구현`)
4. 관련 이슈 번호 포함 (예: `Closes #12`)
5. 최소 1명 리뷰 승인 후 머지

> **주의:** `application-prod.yml`에 실제 비밀번호·시크릿 키를 직접 작성하지 마세요.

---

## 라이선스

This project is licensed under the MIT License.

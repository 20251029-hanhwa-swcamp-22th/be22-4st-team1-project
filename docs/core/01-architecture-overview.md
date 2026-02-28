# MapLog 아키텍처 개요

> **한 줄 요약**: MapLog는 위치 기반 소셜 다이어리 서비스로, CQRS 패턴을 통한 도메인 분리, 다형성 파일 저장소, SSE 기반 실시간 알림 등의 핵심 기술로 확장 가능한 웹 애플리케이션입니다.

## 목차 (Table of Contents)

- [시스템 개요](#시스템-개요)
- [고수준 아키텍처](#고수준-아키텍처)
- [백엔드 도메인 설계](#백엔드-도메인-설계)
- [프론트엔드 구조](#프론트엔드-구조)
- [데이터 흐름](#데이터-흐름)
- [기술 스택](#기술-스택)

---

## 시스템 개요

### 핵심 기능

MapLog는 사용자가 방문한 위치를 지도에 마킹하고 일기를 작성하며, 친구들과 실시간으로 일기를 공유하는 위치 기반 소셜 다이어리 애플리케이션입니다.

**주요 기능:**
1. **회원 관리**: 회원가입, 로그인, 로그아웃, JWT 기반 인증
2. **일기 관리**: 지도 기반 위치 마킹, 다중 이미지 업로드, 공개 범위 설정 (PUBLIC/PRIVATE/FRIENDS_ONLY)
3. **친구 기능**: 친구 요청, 수락, 거절, 친구 피드 조회
4. **실시간 알림**: SSE를 통한 친구 요청, 수락, 일기 공유 알림
5. **스크랩**: 다른 사용자의 일기를 스크랩하여 보관

### 배포 구성

```
Internet
   ↓
Nginx Ingress (Port 80)
   ├─→ Frontend Service (Vue 3 + Vite)
   └─→ Backend Service (Spring Boot 8080)
       └─→ Database (MariaDB 3306)
           └─→ File Storage (Local / AWS S3)
```

---

## 고수준 아키텍처

### 계층적 구조 (Layered Architecture)

```
┌─────────────────────────────────────────────────────────────┐
│                    Presentation Layer                         │
│  (Vue 3 Components, Router, Axios Interceptors)              │
└────────────────────────┬────────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────────┐
│                 API Gateway / Routing                        │
│  (Nginx Reverse Proxy, CORS, Load Balancing)                │
└────────────────────────┬────────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────────┐
│                  Backend Service Layer                        │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │  Command/Query Controllers (REST API Endpoints)         │ │
│  │  - /api/auth, /api/users, /api/diaries, /api/friends   │ │
│  └─────────────────────────────────────────────────────────┘ │
│                         │                                     │
│  ┌─────────────────────▼─────────────────────────────────┐ │
│  │       Business Logic Layer (Services)                  │ │
│  │  - AuthService, DiaryCommandService, FriendService    │ │
│  │  - NotificationCommandService, SseEmitterService      │ │
│  └─────────────────────────────────────────────────────────┘ │
│                         │                                     │
│  ┌─────────────────────▼─────────────────────────────────┐ │
│  │    Data Access Layer (JPA + MyBatis)                   │ │
│  │  - Repository, JPA Entity, MyBatis Mapper             │ │
│  └─────────────────────────────────────────────────────────┘ │
│                         │                                     │
│  ┌─────────────────────▼─────────────────────────────────┐ │
│  │    Cross-Cutting Concerns                              │ │
│  │  - Security (JWT Filter), Exception Handler            │ │
│  │  - File Storage Strategy, Logging                      │ │
│  └─────────────────────────────────────────────────────────┘ │
└────────────────────────┬────────────────────────────────────┘
                         │
        ┌────────────────┼────────────────┐
        ▼                ▼                ▼
    ┌────────┐      ┌────────┐      ┌────────────┐
    │ MariaDB│      │Local FS│      │  AWS S3    │
    │ (Data) │      │(Dev)   │      │  (Prod)    │
    └────────┘      └────────┘      └────────────┘
```

### CQRS (Command Query Responsibility Segregation) 패턴

MapLog 백엔드는 각 도메인을 **command**(쓰기) 와 **query**(읽기) 로 분리하여 구현합니다.

**Command 모듈** (쓰기 작업):
- 컨트롤러, 서비스, 도메인, 레포지토리 포함
- POST, PUT, DELETE 요청 처리
- 트랜잭션 관리, 비즈니스 로직 구현

**Query 모듈** (읽기 작업):
- 컨트롤러, 서비스, MyBatis Mapper 포함
- GET 요청 처리
- 조인, 필터링, 페이지네이션 등 복잡한 조회 로직

**예시: Diary 도메인**
```
diary/
├── command/
│   ├── controller/DiaryCommandController.java     (POST, PUT, DELETE)
│   ├── service/DiaryCommandService.java           (생성, 수정, 삭제 로직)
│   ├── domain/Diary.java                          (Entity)
│   ├── repository/DiaryCommandRepository.java     (JPA)
│   └── dto/CreateDiaryRequest, UpdateDiaryRequest
└── query/
    ├── controller/DiaryQueryController.java       (GET)
    ├── service/DiaryQueryService.java             (조회 로직)
    ├── mapper/DiaryQueryMapper.java               (MyBatis)
    └── dto/DiaryDetailResponse, DiarySummaryResponse
```

---

## 백엔드 도메인 설계

### 1. User Domain (사용자, 인증)

**목표**: 회원 관리 및 JWT 기반 인증

**주요 엔티티**:
- `User`: 사용자 정보 (email, password, nickname, profileImageUrl, role, status)
- `Role` (Enum): USER, ADMIN
- `UserStatus` (Enum): ACTIVE, SUSPENDED, DELETED

**주요 API**:
- `POST /api/auth/signup` - 회원가입
- `POST /api/auth/login` - 로그인
- `POST /api/auth/refresh` - 토큰 갱신
- `POST /api/auth/logout` - 로그아웃
- `GET /api/users/me` - 내 정보 조회
- `PUT /api/users/me` - 프로필 수정
- `DELETE /api/users/me` - 계정 삭제

**보안**:
- 비밀번호는 BCrypt로 해싱
- JWT 토큰: Access (30분) + Refresh (14일)
- `JwtAuthenticationFilter`로 모든 요청 검증

---

### 2. Diary Domain (일기)

**목표**: 위치 기반 일기 작성, 공유, 스크랩

**주요 엔티티**:
- `Diary`: 일기 (title, content, latitude, longitude, locationName, address, visitedAt, visibility)
- `DiaryImage`: 일기 이미지 (imageUrl)
- `Visibility` (Enum): PUBLIC, PRIVATE, FRIENDS_ONLY
- `DiaryShare`: 일기 공유 (diaryId, targetUserId)
- `Scrap`: 일기 스크랩 (userId, diaryId)

**주요 API**:
- `POST /api/diaries` - 일기 작성 (다중 이미지 업로드)
- `GET /api/diaries/{id}` - 일기 상세 조회
- `PUT /api/diaries/{id}` - 일기 수정
- `DELETE /api/diaries/{id}` - 일기 삭제
- `GET /api/diaries/markers` - 지도 마커 조회
- `GET /api/diaries?paginate` - 내 일기 목록
- `POST /api/diaries/{id}/scraps` - 스크랩 추가
- `DELETE /api/diaries/{id}/scraps` - 스크랩 삭제

**접근 제어**:
- PRIVATE: 작성자만 조회 가능
- FRIENDS_ONLY: 작성자 + 친구 조회 가능
- PUBLIC: 모든 인증 사용자 조회 가능

---

### 3. Friend Domain (친구)

**목표**: 친구 요청, 관계 관리, 피드

**주요 엔티티**:
- `Friend`: 친구 관계 (requesterId, receiverId, status)
- `FriendStatus` (Enum): PENDING, ACCEPTED, REJECTED

**주요 API**:
- `POST /api/friends/requests` - 친구 요청 발송
- `POST /api/friends/requests/{id}/accept` - 친구 요청 수락
- `POST /api/friends/requests/{id}/reject` - 친구 요청 거절
- `DELETE /api/friends/{id}` - 친구 삭제
- `GET /api/friends` - 친구 목록
- `GET /api/friends/pending` - 보류 중인 친구 요청
- `GET /api/feed` - 친구들의 공개 일기 피드

**피드 로직**:
- 친구 관계가 ACCEPTED인 사용자의 FRIENDS_ONLY/PUBLIC 일기만 표시

---

### 4. Notification Domain (알림)

**목표**: 소셜 인터랙션 알림 전송 및 저장

**주요 엔티티**:
- `Notification`: 알림 (userId, type, message, referenceId, isRead)
- `NotificationType` (Enum): FRIEND_REQUEST, FRIEND_ACCEPTED, DIARY_SHARED

**주요 API**:
- `GET /api/notifications` - 알림 목록 (읽음 필터링)
- `POST /api/notifications/{id}/read` - 알림 읽음 표시
- `POST /api/notifications/read-all` - 모든 알림 읽음 처리
- `DELETE /api/notifications/{id}` - 알림 삭제
- `DELETE /api/notifications` - 알림 일괄 삭제

**SSE 연동**:
- 알림 생성 시 즉시 `SseEmitterService`를 통해 실시간 푸시

---

### 5. SSE (Server-Sent Events) Domain

**목표**: 실시간 알림 전송

**주요 컴포넌트**:
- `SseEmitterService`: 사용자별 활성 SSE 연결 관리 (ConcurrentHashMap)
- `/api/sse/subscribe` (GET): SSE 연결 수립, 쿼리 파라미터 `?token=...`로 JWT 전달
- 30분 타임아웃 설정으로 좀비 연결 방지

**메시지 형식**:
```
event: notification
data: {"type":"FRIEND_REQUEST","message":"..."}
```

---

## 프론트엔드 구조

### 상태 관리 (Pinia)

```
src/app/
├── stores/
│   ├── auth.js          # 인증 상태 (user, tokens)
│   └── notification.js  # 알림 상태 (count, list)
```

**auth.js 주요 상태**:
- `user`: { userId, email, nickname, role, profileImageUrl, createdAt }
- `accessToken`, `refreshToken`
- `isAuthenticated` (computed)
- `isAdmin` (computed)

**주요 액션**:
- `login(credentials)` - 로그인, 토큰 저장, 사용자 정보 조회
- `signup(payload)` - 회원가입
- `hydrateUser()` - 새로고침 시 사용자 정보 복구
- `logout()` - 로그아웃, 토큰 및 상태 초기화

---

### API 모듈 (Axios)

```
src/app/api/
├── axios.js         # 공유 인스턴스 + 인터셉터
├── auth.js          # 인증 API
├── user.js          # 사용자 API
├── diary.js         # 일기 API
├── friend.js        # 친구 API
├── feed.js          # 피드 API
├── notification.js  # 알림 API
└── admin.js         # 관리자 API
```

**Axios 인터셉터 (axios.js)**:

1. **요청 인터셉터**: localStorage에서 accessToken 자동 추출, Authorization 헤더 첨부
2. **응답 인터셉터**:
   - 201 응답 자동 unwrap (ApiResponse.data 반환)
   - 401 상태 시 토큰 갱신 로직:
     - `isRefreshing` 플래그로 동시 갱신 방지
     - `pendingQueue`에 대기 중인 요청 큐잉
     - `/api/auth/refresh`로 새 토큰 획득
     - 실패 시 localStorage 초기화 및 /login 리다이렉트

---

### 라우팅 및 가드

```
src/app/router/
├── index.js         # 라우트 정의, 라우트 가드
```

**라우트 가드**:
- `requireAuth`: 인증 필요한 페이지 (로그인되지 않으면 /login으로)
- `requireGuest`: 로그아웃 페이지 (로그인되면 /map으로)
- `requireAdmin`: 관리자 페이지 (admin 아니면 /map으로)

---

## 데이터 흐름

### 일기 작성 플로우

```
Frontend (Vue Component)
    ↓ 사용자 입력 (제목, 내용, 위치, 이미지)
    ↓ FormData 생성
Frontend (Axios)
    ↓ POST /api/diaries
Backend (DiaryCommandController)
    ↓ multipart/form-data 파싱
Backend (DiaryCommandService)
    ├─ User 조회
    ├─ Diary 생성
    ├─ 이미지 업로드 (FileStorageService)
    │  └─ Local FS 또는 S3 저장
    ├─ DiaryImage 저장 (DB)
    ├─ DiaryShare 저장 (공유 대상)
    └─ NotificationCommandService.createDiarySharedNotification()
        └─ Notification 저장 + SSE 푸시
Backend (Database)
    ↓ Diary, DiaryImage, DiaryShare, Notification 저장
Frontend (Axios Response)
    ↓ 201 Created, diaryId 반환
Frontend (Router)
    ↓ /diary/{diaryId} 리다이렉트
```

---

### 일기 조회 플로우 (CQRS)

```
Frontend (MapView)
    ↓ 지도 범위 변경 (minLat, maxLat, minLng, maxLng)
    ↓ GET /api/diaries/markers?minLat=...&maxLat=...
Backend (DiaryQueryController)
    ↓ 쿼리 파라미터 검증
Backend (DiaryQueryService)
    ├─ User 조회
    ├─ DiaryQueryMapper.findMapMarkers()
    │  └─ MyBatis SQL (LEFT JOIN + WHERE 접근 제어)
    └─ DiaryMarkerResponse[] 반환
Frontend (MapComponent)
    ↓ Kakao Map 마커 렌더링, 클러스터링
```

---

### SSE 실시간 알림 플로우

```
Frontend (App.vue mount)
    ↓ EventSource('/api/sse/subscribe?token=...')
    ↓ 연결 수립
    ↓ 이벤트 리스너 등록

Backend (SSE Event 발생)
    ├─ FriendCommandService.acceptFriendRequest()
    └─ NotificationCommandService.createFriendAcceptedNotification()
       └─ SseEmitterService.send(receiverId, 'notification', {...})
          ├─ ConcurrentHashMap에서 receiverId의 SseEmitter 조회
          └─ emitter.send(SseEvent)

Frontend (EventSource listener)
    ↓ event: notification 수신
    ↓ Pinia notification store 업데이트
    ↓ 헤더의 알림 종 아이콘 갱신 (count++)
```

---

## 기술 스택

### Backend
- **Framework**: Spring Boot 3.5
- **Language**: Java 21
- **Build**: Gradle
- **Database**: MariaDB 11
- **ORM**: Spring Data JPA + Hibernate
- **Query**: MyBatis (복잡한 조회 쿼리)
- **Security**: Spring Security + JWT (jjwt library)
- **File Storage**: AWS S3 (Production) / Local FS (Development)
- **Real-time**: Server-Sent Events (SSE)

### Frontend
- **Framework**: Vue 3 (Composition API)
- **Build Tool**: Vite
- **State Management**: Pinia
- **HTTP Client**: Axios
- **Map API**: Kakao Maps SDK
- **Styling**: CSS (Tailwind CSS 선택사항)
- **Testing**: Vitest

### Infrastructure
- **Containerization**: Docker + Docker Compose
- **Reverse Proxy**: Nginx
- **Container Orchestration**: Kubernetes (Production)
- **CI/CD**: Jenkins + ArgoCD
- **Cloud Storage**: AWS S3

---

## 핵심 설계 결정사항

### 1. CQRS 패턴 도입
**이유**: 읽기와 쓰기 작업의 복잡도 차이를 명확히 분리하여 각각 최적화

**장점**:
- 읽기 최적화 (MyBatis로 복잡한 JOIN 처리)
- 쓰기 단순화 (도메인 로직 집중)
- 확장성 (필요시 읽기/쓰기 DB 분리 가능)

### 2. 다형성 파일 저장소 (Strategy Pattern)
**이유**: 로컬 개발과 AWS S3 운영 환경을 동일한 인터페이스로 관리

**구현**:
```java
public interface FileStorageService {
    String store(MultipartFile file);
    void delete(String url);
    String generatePresignedUrl(String url);
}

@Component
@Profile("dev")
public class LocalFileStorageService implements FileStorageService { ... }

@Component
@Profile("aws")
public class S3FileStorageService implements FileStorageService { ... }
```

### 3. SSE 기반 실시간 알림
**이유**: WebSocket의 양방향 통신은 오버엔지니어링, 단방향 푸시만 필요

**장점**:
- HTTP 기반으로 방화벽 친화적
- 설정 간단 (HTML5 EventSource)
- 브라우저 자동 재연결 지원

### 4. JWT Refresh Token 전략
**이유**: Access Token 유효 기간 단축으로 보안 강화

**구현**:
- Access Token: 30분 (짧은 유효기간)
- Refresh Token: 14일 (긴 유효기간)
- Axios 인터셉터로 자동 갱신

---

## 참고 자료

- [Spring Boot 공식 문서](https://spring.io/projects/spring-boot)
- [Vue 3 공식 문서](https://vuejs.org/)
- [CQRS 패턴 (Martin Fowler)](https://martinfowler.com/bliki/CQRS.html)
- [Server-Sent Events MDN](https://developer.mozilla.org/en-US/docs/Web/API/Server-sent_events)
- [JWT.io](https://jwt.io/)
- [Pinia 공식 문서](https://pinia.vuejs.org/)

---

**다음 문서 참고:**
- [API 설계 문서](./02-api-design.md) - 모든 엔드포인트의 상세 사양
- [데이터베이스 스키마](./03-database-schema.md) - 테이블 정의 및 관계
- [개발 환경 설정](./04-development-setup.md) - 로컬 개발 가이드
- [배포 가이드](./05-deployment.md) - 프로덕션 배포 절차
- [인증/보안 설계](./06-auth-security.md) - JWT, 암호화, 접근 제어

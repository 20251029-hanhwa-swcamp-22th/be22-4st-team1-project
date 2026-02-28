# MapLog 핵심 기술 문서

MapLog는 위치 기반 소셜 다이어리 웹 애플리케이션입니다. 본 문서 모음은 MapLog의 전체 기술 스택, 아키텍처, 개발/배포 가이드를 포괄적으로 다룹니다.

## 문서 목차

### 1. 📐 [아키텍처 개요](./01-architecture-overview.md)

**대상**: 아키텍처를 이해하고 싶은 모든 개발자

MapLog의 전체 시스템 아키텍처를 다룹니다:
- 고수준 계층 구조 (Presentation, Application, Data Access)
- CQRS 패턴을 통한 도메인 분리
- 5개 주요 도메인 (User, Diary, Friend, Notification, SSE)
- 다형성 파일 저장소 (로컬 FS vs AWS S3)
- 프론트엔드 상태 관리 (Pinia)
- 핵심 설계 결정사항

**핵심 요점**:
```
- CQRS 패턴으로 읽기/쓰기 로직 분리
- JWT 토큰 (30분 Access + 14일 Refresh)
- SSE를 통한 실시간 알림
- 전략 패턴으로 파일 저장소 추상화
```

---

### 2. 🔌 [API 설계 문서](./02-api-design.md)

**대상**: API 개발, 통합, 테스트를 하는 개발자

모든 REST API 엔드포인트의 상세 사양을 제공합니다:
- 응답 형식 (통일된 ApiResponse<T>)
- 인증/권한 정책 (JWT, Role-based)
- 9개 리소스 도메인별 엔드포인트 (Auth, Users, Diaries, Friends, Feed, Notifications, Scraps, SSE, Admin)
- 요청/응답 예시
- 오류 처리 (ErrorCode 30가지)
- 페이지네이션

**빠른 참조**:
| 엔드포인트 | 메서드 | 인증 |
|----------|--------|------|
| `/api/auth/login` | POST | ❌ |
| `/api/users/me` | GET | ✅ |
| `/api/diaries` | POST | ✅ |
| `/api/friends/requests` | POST | ✅ |
| `/api/notifications` | GET | ✅ |
| `/api/sse/subscribe` | GET | ✅ (쿼리 파라미터) |

---

### 3. 🗄️ [데이터베이스 스키마](./03-database-schema.md)

**대상**: 데이터베이스 설계, 쿼리 최적화, 마이그레이션을 하는 개발자

MariaDB 11 스키마의 전체 정의와 관계도:
- 7개 주요 테이블 (users, diaries, diary_images, diary_shares, scraps, friends, notifications)
- 각 테이블의 칼럼 정의 및 제약 조건
- ER 다이어그램 (텍스트 형식)
- 인덱싱 전략 (복합 인덱스, UNIQUE 제약)
- 외래 키 관계 (CASCADE 삭제)

**테이블 관계**:
```
users (1) ──────────── (N) diaries
  │                        │
  ├─ (1:N) ──→ diary_shares
  ├─ (1:N) ──→ scraps
  ├─ (1:N) ──→ friends (requester_id / receiver_id)
  └─ (1:N) ──→ notifications

diaries (1) ───────── (N) diary_images
          └────── (N) scraps
```

---

### 4. ⚙️ [개발 환경 설정](./04-development-setup.md)

**대상**: 로컬 개발 환경을 구축하려는 개발자

처음부터 끝까지 개발 환경 세팅 가이드:
- 사전 요구사항 (Java 21, Node.js, Docker)
- 설치 단계 (OS별: Windows, macOS, Linux)
- 프로젝트 설정 (.env, 환경 변수)
- 프로젝트 실행 (Docker Compose 또는 로컬 3개 터미널)
- 개발 기본 명령어 (Gradle, npm)
- 문제 해결 (포트 충돌, DB 연결 실패 등)
- IDE 설정 (IntelliJ IDEA, VS Code)
- 개발 워크플로우 (브랜치 생성부터 PR까지)

**빠른 시작**:
```bash
# 1단계: 저장소 클론
git clone https://github.com/20251029-hanhwa-swcamp-22th/be22-4st-team1-project.git

# 2단계: 환경 변수 설정
cp .env.example .env
# .env 파일 수정

# 3단계: 데이터베이스 시작
docker-compose up -d mariadb

# 4단계: 백엔드 실행 (터미널 1)
cd map-log-backend && ./gradlew bootRun

# 5단계: 프론트엔드 실행 (터미널 2)
cd map-log-frontend && npm install && npm run dev

# 6단계: 웹브라우저
http://localhost:5173
```

---

### 5. 🚀 [배포 가이드](./05-deployment.md)

**대상**: 로컬 개발 이외의 모든 환경에 배포하는 개발자/DevOps

Docker, Kubernetes, CI/CD 파이프라인을 통한 배포 전략:
- 배포 아키텍처 (개발: Docker Compose, 프로덕션: Kubernetes)
- Dockerfile 멀티 스테이지 빌드 (백엔드, 프론트엔드)
- Nginx 설정 (API 프록시, SPA 라우팅, SSE 지원)
- Docker Compose 프로덕션 설정
- Kubernetes 배포 (Deployment, StatefulSet, Service, Ingress)
- Jenkins CI 파이프라인 (Webhook, 빌드, 도커 푸시)
- ArgoCD CD (GitOps, 자동 배포)
- 모니터링 및 로깅 (헬스 체크, 애플리케이션 로그)

**배포 흐름**:
```
개발자 코드 푸시
  ↓
GitHub Webhook
  ↓
Jenkins CI (빌드 → Docker 이미지 생성 → 레지스트리 푸시)
  ↓
GitOps 매니페스트 저장소 업데이트
  ↓
ArgoCD (감지 → Kubernetes 배포)
  ↓
무중단 배포 완료
```

---

### 6. 🔐 [인증/보안 설계](./06-auth-security.md)

**대상**: 보안 정책, 권한 관리, 취약점 방어를 담당하는 개발자

인증, 암호화, 접근 제어, 보안 취약점 방어:
- JWT 토큰 관리 (구조, 검증, 갱신)
- BCrypt 비밀번호 해싱 (강도, 소금)
- 역할 기반 접근 제어 (RBAC) (@PreAuthorize 어노테이션)
- CORS 정책 및 프리플라이트 요청
- 보안 취약점 6가지 (SQL Injection, XSS, CSRF, Rate Limiting, 비밀번호 정책, SSL/TLS)
- API 보안 (입력 검증, 보안 헤더)
- 감사 로깅 (Audit Trail)

**보안 체크리스트**:
- [x] JWT 시크릿 최소 32자
- [x] 비밀번호 BCrypt 해싱 (Round 10)
- [x] Access Token 30분 (짧음)
- [x] Refresh Token 14일 (길음)
- [x] 역할 기반 권한 검사 (@PreAuthorize)
- [x] CORS 설정 (호스트 화이트리스트)
- [x] SQL Injection 방어 (Parameterized Queries)
- [x] XSS 방어 (JSON 자동 이스케이프, DomPurify)
- [x] HTTPS/TLS 설정
- [x] 감사 로깅

---

### 7. 🎨 [코딩 스타일 가이드](./07-coding-style-guide.md)

**대상**: 모든 개발자 (백엔드 Java, 프론트엔드 JavaScript/Vue)

일관된 코드 품질 유지를 위한 명명 규칙, 작성 패턴, 주석 스타일:
- 공통 규칙 (UTF-8 인코딩, 라인 길이, 주석)
- Java 스타일 (패키지/클래스/메서드 명명, 필드 순서, Exception 처리)
- JavaScript/Vue 스타일 (camelCase, 파일 구조, Composition API 패턴)
- SQL 작성 규칙 (MyBatis Mapper)
- Git Commit 메시지 (Conventional Commits)

**빠른 참조**:
```
명명 규칙:
- Java 클래스: PascalCase (DiaryCommandController)
- JavaScript 파일: kebab-case (diary-detail.vue)
- 상수: UPPER_SNAKE_CASE (MAX_TITLE_LENGTH)
- 메서드: 도메인 동작 명령형 (createDiary, updateProfile)

Commit 메시지:
feat(diary): 일기 공유 기능 추가
fix(auth): 토큰 갱신 실패 문제 해결
docs: API 설계 문서 업데이트
```

---

### 8. 🧪 [테스트 가이드](./08-testing-guide.md)

**대상**: 테스트 코드를 작성하고 품질을 검증하는 개발자

백엔드(JUnit 5, Mockito) 및 프론트엔드(Vitest) 테스트 전략:
- 테스트 피라미드 (단위/통합/E2E)
- 백엔드: 단위 테스트(서비스, Entity), 통합 테스트(Controller)
- 프론트엔드: Store 테스트(Pinia), API 테스트(Axios), 컴포넌트 테스트(Vue)
- Mock 및 Stub 사용법
- 테스트 유틸리티 및 헬퍼 함수
- CI/CD 통합 (GitHub Actions)

**테스트 커버리지 목표**:
```
- 백엔드: 70% 이상
- 프론트엔드: 60% 이상
- 핵심 비즈니스 로직: 90% 이상
```

---

### 9. 🤝 [기여 가이드](./09-contributing-guide.md)

**대상**: 프로젝트에 새로운 기능을 추가하거나 개선하는 모든 개발자

처음부터 끝까지 기여 프로세스:
- 로컬 환경 설정 및 IDE 구성
- 이슈 생성 및 검토
- 브랜치 생성 (도메인/기능명)
- 백엔드/프론트엔드/문서 구현
- 테스트 작성
- Git 커밋 (컨벤션 따르기)
- PR 생성 및 코드 리뷰
- 배포 프로세스

**전체 플로우**:
```
Issue → Branch → Code → Test → Commit → PR → Review → Deploy
```

---

### 10. ⚡ [성능 최적화 가이드](./10-performance-optimization.md)

**대상**: 데이터베이스, API, 프론트엔드 성능을 개선하는 개발자

최적화 기법 및 모니터링:
- 백엔드: N+1 쿼리 해결, 인덱싱 전략, 캐싱, 커넥션 풀
- 프론트엔드: 이미지 압축, 번들 최적화, API 배칭, 가상 스크롤링
- 인프라: Nginx 압축/캐싱, Docker 최적화, 읽기 복제본
- 성능 모니터링 (Spring Boot Actuator, Performance Observer)
- 성능 벤치마크 및 부하 테스트

**성능 목표**:
| 메트릭 | 목표 |
|--------|------|
| API 응답 | < 200ms |
| 페이지 로드 | < 3초 |
| DB 쿼리 | < 100ms |

---

## 문서 사용 방법

### 🎯 역할별 추천 문서

**신규 개발자 (온보딩)**:
1. 📐 아키텍처 개요 - 전체 그림 파악
2. ⚙️ 개발 환경 설정 - 로컬 환경 구축
3. 🎨 코딩 스타일 가이드 - 명명 규칙 학습
4. 🔌 API 설계 문서 - 엔드포인트 학습
5. 🤝 기여 가이드 - 첫 PR 준비

**백엔드 개발자**:
1. 📐 아키텍처 개요 - 도메인 이해
2. 🗄️ 데이터베이스 스키마 - ERD, 쿼리 최적화
3. 🔐 인증/보안 설계 - 권한 관리
4. 🎨 코딩 스타일 가이드 - Java 명명 규칙
5. 🧪 테스트 가이드 - 단위 테스트 작성
6. ⚡ 성능 최적화 가이드 - 쿼리 최적화

**프론트엔드 개발자**:
1. 🔌 API 설계 문서 - 엔드포인트 명세
2. 📐 아키텍처 개요 - Pinia 상태 관리
3. ⚙️ 개발 환경 설정 - 로컬 개발
4. 🎨 코딩 스타일 가이드 - Vue/JavaScript 패턴
5. 🧪 테스트 가이드 - Vitest 작성법
6. ⚡ 성능 최적화 가이드 - 번들 최적화, 이미지 압축

**QA/테스트**:
1. 🔌 API 설계 문서 - 테스트 케이스 설계
2. 🧪 테스트 가이드 - 자동화 테스트
3. ⚡ 성능 최적화 가이드 - 벤치마크

**DevOps/인프라**:
1. 🚀 배포 가이드 - 배포 전략
2. 📐 아키텍처 개요 - 시스템 구조
3. 🔐 인증/보안 설계 - CORS, SSL/TLS, 환경변수
4. ⚡ 성능 최적화 가이드 - Nginx, Kubernetes 최적화

**프로젝트 관리/리드**:
1. 📐 아키텍처 개요 - 전체 기술 스택
2. 🤝 기여 가이드 - 개발 프로세스
3. 🎨 코딩 스타일 가이드 - 품질 기준
4. 🚀 배포 가이드 - 릴리스 계획

---

### 📚 교차 참조 맵

```
아키텍처 개요
  ├─ 도메인 분리 (CQRS)
  │   └─ 데이터베이스 스키마
  ├─ API 설계
  │   └─ API 설계 문서
  ├─ 인증/보안
  │   └─ 인증/보안 설계
  └─ 배포 구조
      └─ 배포 가이드

개발 환경 설정
  ├─ 로컬 개발 시작
  │   └─ API 설계 문서 (테스트용)
  └─ 문제 해결
      └─ Troubleshooting 섹션

배포 가이드
  ├─ Docker 빌드
  ├─ Kubernetes 배포
  │   ├─ 아키텍처 개요 (시스템 구조)
  │   └─ 인증/보안 설계 (환경 변수)
  └─ CI/CD 파이프라인
```

---

## 기술 스택 빠른 참조

| 계층 | 기술 | 버전 | 용도 |
|------|------|------|------|
| **백엔드** | Spring Boot | 3.5 | 웹 프레임워크 |
|  | Java | 21 | 프로그래밍 언어 |
|  | Spring Data JPA | 3.5 | ORM |
|  | MyBatis | 3.0 | 복잡 쿼리 |
|  | Spring Security | 3.5 | 인증/인가 |
|  | JWT (jjwt) | 0.12 | 토큰 관리 |
|  | Gradle | 8.0+ | 빌드 도구 |
| **프론트엔드** | Vue | 3 | 웹 UI 프레임워크 |
|  | Vite | 5 | 번들러 |
|  | Pinia | 2 | 상태 관리 |
|  | Axios | 1.6 | HTTP 클라이언트 |
|  | Node.js | 20 LTS | 런타임 |
|  | npm | 10 | 패키지 관리자 |
| **데이터베이스** | MariaDB | 11 | RDBMS |
|  | Hibernate | 6.2 | JPA 구현체 |
| **인프라** | Docker | 20.10+ | 컨테이너 |
|  | Docker Compose | 2.0+ | 다중 컨테이너 |
|  | Kubernetes | 1.28+ | 오케스트레이션 |
|  | Nginx | 1.24 | 리버스 프록시 |
|  | Jenkins | 2.4+ | CI |
|  | ArgoCD | 2.8+ | CD (GitOps) |
| **외부 서비스** | AWS S3 | - | 이미지 스토리지 |
|  | Kakao Maps API | - | 지도 (프론트엔드) |

---

## 자주 묻는 질문 (FAQ)

### Q1: JWT 시크릿은 어디에 보관해야 하나요?

**A**: `.env` 파일이나 환경 변수로 관리하되, 절대 버전 관리에 포함시키면 안 됩니다.

```bash
# .gitignore
.env
.env.local
```

참고: [인증/보안 설계 - JWT 시크릿 보안](./06-auth-security.md#jwt-시크릿-보안)

---

### Q2: 로컬 개발 시 AWS S3 없이 파일을 업로드할 수 있나요?

**A**: 네, 개발 환경은 자동으로 로컬 파일 시스템(`uploads/`)을 사용합니다.

```yaml
# application-dev.yml 자동 설정
spring:
  profiles:
    active: dev  # LocalFileStorageService 활성화
```

참고: [아키텍처 개요 - 다형성 파일 저장소](./01-architecture-overview.md#2-다형성-파일-저장소-strategy-pattern)

---

### Q3: SSE 실시간 알림이 연결되지 않아요.

**A**: 가장 일반적인 원인은 JWT 토큰이 쿼리 파라미터로 올바르게 전달되지 않는 것입니다.

```javascript
// ✅ 올바른 사용
const token = localStorage.getItem('ml_access_token')
const eventSource = new EventSource(`/api/sse/subscribe?token=${token}`)

// ❌ 잘못된 사용
const eventSource = new EventSource('/api/sse/subscribe')  // 토큰 없음
```

참고: [아키텍처 개요 - SSE 플로우](./01-architecture-overview.md#sse-실시간-알림-플로우)

---

### Q4: 데이터베이스 쿼리 성능을 개선하려면?

**A**: `idx_latitude_longitude` 인덱스가 지도 조회 성능을 큰 영향을 미칩니다. 필요시 추가 인덱스를 고려하세요.

참고: [데이터베이스 스키마 - 인덱싱 전략](./03-database-schema.md#인덱싱-전략)

---

### Q5: 새로운 권한 레벨을 추가하려면?

**A**:
1. `Role` enum에 새 역할 추가
2. `SecurityConfig`에서 경로별 권한 설정
3. 필요시 데이터베이스 마이그레이션

참고: [인증/보안 설계 - RBAC](./06-auth-security.md#접근-제어-rbac)

---

## 커뮤니케이션 가이드

### 버그 리포팅

- **작은 버그** (오타, 명백한 오류): GitHub Issue에 직접 작성
- **보안 취약점**: 팀 리드에게 비공개로 보고
- **아키텍처 문제**: 팀 미팅에서 논의

### 문서 업데이트 요청

- PR을 통해 수정 제안 (마크다운 형식 유지)
- 기술 정확성 검수는 해당 도메인 담당자가 진행

---

## 변경 로그

| 버전 | 날짜 | 변경사항 |
|------|------|---------|
| 2.0 | 2026-02-28 | 추가 문서화 완료 (4개 문서 추가: 스타일, 테스트, 기여, 성능) |
| 1.0 | 2025-02-28 | 초기 문서화 완료 (6개 문서: 아키텍처, API, DB, 설정, 배포, 보안) |

### 버전 2.0에서 추가된 문서

- **07-coding-style-guide.md**: Java, JavaScript/Vue, SQL 코딩 스타일 및 Git 컨벤션
- **08-testing-guide.md**: 백엔드 JUnit 5, 프론트엔드 Vitest 테스트 작성 가이드
- **09-contributing-guide.md**: 신규 기능 추가부터 배포까지의 완전한 프로세스
- **10-performance-optimization.md**: 데이터베이스, API, 프론트엔드 성능 최적화 기법

---

## 라이센스

이 문서 모음은 MapLog 프로젝트의 일부이며, 팀 내부 사용을 위해 작성되었습니다.

---

**마지막 업데이트**: 2026-02-28

---

## 문서 기여

이 문서들을 개선하고 싶으신가요? 기여 방법:

1. 오타나 부정확한 내용 발견 시 → PR 제출
2. 새로운 섹션 추가 제안 → Issue 등록 후 논의
3. 예시 코드 개선 → PR로 제출

모든 문서는 한국어로 작성되며, 기술 용어는 영문 병기합니다.

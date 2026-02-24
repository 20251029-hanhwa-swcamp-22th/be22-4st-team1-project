# 이미지 로딩 401 Unauthorized (K8s 배포 환경)

## 증상

K8s(ArgoCD) 배포 환경에서 일기 상세 페이지 진입 시 이미지가 로드되지 않고 브라우저 콘솔에 아래 에러 발생.

```
GET http://localhost/api/uploads/73d810f7-38ff-4e8a-9c8e-869ee7b2ce3c.png 401 (Unauthorized)
```

백엔드 로그:

```
Securing GET /api/uploads/73d810f7-38ff-4e8a-9c8e-869ee7b2ce3c.png
AnonymousAuthenticationFilter: Set SecurityContextHolder to anonymous SecurityContext
```

---

## 원인 분석

### 에러 연쇄 흐름

```
DB에 저장된 imageUrl: /uploads/73d810f7...png   ← LocalFileStorageService 형식
        ↓
DiaryDetailView.vue - toImageUrl('/uploads/...')
  VITE_API_BASE_URL = /api
  → base + path = /api/uploads/uuid.png
        ↓
Nginx: /api/* → backend:8080 프록시
        ↓
Spring Security: /uploads/** 만 permitAll, /api/uploads/** 는 미허용
        ↓
<img> 태그 요청에는 Authorization 헤더 없음 → 익명 사용자 처리
        ↓
401 Unauthorized
```

### 핵심 원인 3가지

**원인 1. 잘못된 Spring 프로필 활성화**

`application.yml`의 기본 프로필이 `dev`로 설정되어 있고, K8s 배포 시 `SPRING_PROFILES_ACTIVE` 환경변수가 별도로 지정되지 않아 `LocalFileStorageService`가 활성화됨.
S3를 써야 하는 환경에서 이미지가 Pod 로컬 파일시스템(`/uploads/`)에 저장됨.

**원인 2. `toImageUrl` 함수의 잘못된 base URL 적용**

`VITE_API_BASE_URL=/api` 환경에서 상대 경로(`/uploads/...`)에 `/api`가 붙어 `/api/uploads/...`가 됨.
`<img>` 태그는 axios interceptor를 거치지 않으므로 `Authorization` 헤더가 전송되지 않음.

**원인 3. `@Profile` 어노테이션과 `spring.config.import` 혼용 오해**

`application-dev.yml`이 `application-aws.yml`을 import하여 S3 설정값은 로드되지만,
`@Profile("aws")`가 붙은 `S3FileStorageService`는 활성 프로필에 `aws`가 없으면 Bean으로 등록되지 않음.
→ 설정값 로드와 Bean 등록은 별개의 레이어

```
spring.config.import  →  설정값(properties) 로드
@Profile              →  Bean 생성 여부 결정  (독립적으로 동작)
```

---

## 해결 방법

### 스토리지 서비스 프로필 재설계

| 서비스 | 변경 전 | 변경 후 | 용도 |
|---|---|---|---|
| `S3FileStorageService` | `@Profile("aws")` | `@Profile({"dev", "aws"})` | K8s(dev) + 프로덕션(aws) |
| `LocalFileStorageService` | `@Profile("dev")` | `@Profile("local")` | 로컬 개발 전용 |

**`S3FileStorageService.java`**

```java
// 변경 전
@Profile("aws")

// 변경 후
@Profile({"dev", "aws"})
```

**`LocalFileStorageService.java`**

```java
// 변경 전
@Profile("dev")

// 변경 후
@Profile("local")
```

### `application-local.yml` 신규 생성

로컬 개발 전용 설정 파일 분리. 로컬 MariaDB 접속 정보 및 파일 업로드 경로 포함.

### 환경별 프로필 정리

| 환경 | `SPRING_PROFILES_ACTIVE` | 활성화 서비스 |
|---|---|---|
| 로컬 개발 | `local` | `LocalFileStorageService` |
| K8s (dev) | `dev` | `S3FileStorageService` |
| 프로덕션 | `aws` | `S3FileStorageService` |

---

## 이미지 요청 흐름 (수정 후)

```
이미지 업로드 (dev 프로필)
  → S3FileStorageService.store()
  → S3에 diaries/uuid.png 저장
  → DB imageUrl: "https://bucket.s3.amazonaws.com/diaries/uuid.png"
        ↓
getDiaryDetail() 호출
  → generatePresignedUrl("https://...") → 1시간짜리 서명 URL 반환
        ↓
프론트 toImageUrl("https://bucket.s3.amazonaws.com/...?X-Amz-Signature=...")
  → startsWith('https://') → true → 그대로 반환
        ↓
<img src="https://..."> → 브라우저가 S3에 직접 요청
백엔드/Nginx/Spring Security 무관 → 정상 로드
```

---

## 관련 파일

- `map-log-backend/src/main/java/com/maplog/common/storage/S3FileStorageService.java`
- `map-log-backend/src/main/java/com/maplog/common/storage/LocalFileStorageService.java`
- `map-log-backend/src/main/resources/application.yml`
- `map-log-backend/src/main/resources/application-dev.yml`
- `map-log-backend/src/main/resources/application-local.yml`
- `map-log-frontend/src/app/views/DiaryDetailView.vue`

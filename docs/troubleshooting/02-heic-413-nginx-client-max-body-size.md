# HEIC 파일 업로드 시 413 Request Entity Too Large

## 증상

`.heic` 확장자 이미지 업로드 시 아래 에러 발생. JPG, PNG 등 다른 이미지 형식은 정상 업로드됨.

```
HTTP 413 Request Entity Too Large
```

재현 조건:
- 다이어리 작성 시 HEIC 이미지 첨부 (iPhone 촬영 사진 등)
- 프로필 이미지 변경 시 HEIC 이미지 첨부
- 파일 크기: ~747KB 이상

---

## 원인 분석

### 에러 연쇄 흐름

```
브라우저
  │ POST /api/diaries  (multipart body: HEIC 파일 + form 필드)
  ▼
Nginx (client_max_body_size 미설정 → 기본값 1MB)
  │ ← 413 Request Entity Too Large 반환  ← 여기서 차단
  ▼ (Spring Boot에 도달하지 못함)
Spring Boot (max-file-size: 10MB, max-request-size: 50MB → 의미 없음)
  ↓
S3FileStorageService
```

### 요청 경로 확인

`map-log-frontend/.env.local`:

```
VITE_API_BASE_URL=/api
```

`VITE_API_BASE_URL=/api` 이므로 모든 API 요청이 Nginx `/api` 블록을 경유함.
Nginx의 `client_max_body_size` 제한이 직접 적용됨.

### 총 요청 크기 계산

multipart/form-data 요청은 파일 바이너리 외에 form 필드 전체가 body에 포함됨.

| 항목 | 크기 |
|---|---|
| HEIC 파일 (1장) | ~765 KB |
| title, content, 위치 정보 등 form 필드 | 수 KB ~ 수십 KB |
| multipart 경계 헤더 | ~1 KB |
| **합계** | **≥ 1 MB 초과 가능** |

추가로 다이어리는 **이미지 최대 5장 동시 업로드** 가능.
HEIC 2장(~747 KB × 2 = ~1.5 MB) 업로드 시 확실히 초과.

### 왜 HEIC만 문제가 되는가?

HEIC는 Apple 기기 촬영 사진 포맷으로 고화질 특성상 파일 크기가 크다.
테스트에 사용된 JPG/PNG는 통상 수십~수백 KB 수준으로 1MB 제한에 여유가 있었던 것.
즉, **파일 형식 자체의 문제가 아닌 파일 크기 문제**이며,
동일 크기의 JPG도 업로드 시 같은 413이 발생한다.

### 설정 불일치

| 레이어 | 설정값 | 비고 |
|---|---|---|
| Nginx `client_max_body_size` | **1MB (기본값)** | ← 미설정으로 기본값 적용 |
| Spring Boot `max-file-size` | 10MB | `application.yml` |
| Spring Boot `max-request-size` | 50MB | `application.yml` |

---

## 해결 방법

`map-log-frontend/nginx.conf` — `server` 블록에 `client_max_body_size` 추가.

```nginx
server {
    listen 80;
    server_name localhost;

    # 파일 업로드 크기 제한 (Spring Boot max-file-size: 10MB와 맞춤)
    client_max_body_size 10m;

    ...
}
```

Spring Boot의 `max-file-size: 10MB`와 동일한 값으로 맞춰 각 레이어가 일관된 제한을 적용하도록 함.

---

## 반영 방법

재빌드 없이 실행 중인 컨테이너에 즉시 반영:

```bash
# nginx.conf를 컨테이너에 복사
docker cp map-log-frontend/nginx.conf maplog-frontend:/etc/nginx/conf.d/default.conf

# Nginx 무중단 설정 리로드
docker exec maplog-frontend nginx -s reload
```

---

## 관련 파일

- `map-log-frontend/nginx.conf`
- `map-log-backend/src/main/resources/application.yml`
- `map-log-frontend/src/app/views/MapView.vue` (이미지 업로드 로직)
- `map-log-backend/src/main/java/com/maplog/common/storage/S3FileStorageService.java`

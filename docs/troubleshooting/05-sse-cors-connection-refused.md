# SSE 연결 CORS 에러 (Cross-Origin 차단)

## 증상

프론트엔드(localhost:5173)에서 백엔드(localhost:8080)의 SSE 엔드포인트에 연결 시도하면 브라우저에서 CORS 에러 발생.

```
Access to resource at 'http://localhost:8080/api/sse/connect?token=...'
from origin 'http://localhost:5173' has been blocked by CORS policy:
No 'Access-Control-Allow-Origin' header is present on the requested resource.
```

일반 REST API 호출(Axios)은 정상 동작하지만 SSE 연결만 실패.

---

## 원인 분석

### 기존 CORS 설정의 범위 문제

기존 `SecurityConfig`의 CORS 설정이 `/api/**` 패턴만 등록되어 있었음.
SSE 연결 경로(`/api/sse/connect`)는 `/api/**`에 매칭되지만, SSE의 `text/event-stream` 응답이
**장시간 유지되는 HTTP 연결**이기 때문에 일반 REST 요청과 다른 CORS 처리가 필요한 경우가 있었음.

```java
// 수정 전: /api/** 만 등록
source.registerCorsConfiguration("/api/**", config);
// → SSE 경로도 매칭은 되지만, 명시적 등록이 없어 일부 환경에서 문제 발생
```

### EventSource의 CORS 특성

`EventSource`는 `credentials` 모드가 기본적으로 `same-origin`이며, Cross-Origin 요청 시:
- 서버가 `Access-Control-Allow-Origin`을 **명시적으로** 반환해야 함
- `Access-Control-Allow-Credentials: true`가 필요
- `Allow-Origin`에 와일드카드(`*`)를 사용하면 credentials와 충돌

---

## 해결 방법

### SecurityConfig에 SSE 경로 명시적 CORS 등록

**`SecurityConfig.java`**

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOriginPatterns(List.of(
            "http://localhost:5173",          // 로컬 개발 (Vite)
            "https://*.ngrok-free.dev"        // 외부 터널링
    ));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("*"));
    config.setAllowCredentials(true);         // credentials 허용 (필수)
    config.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", config);
    // SSE 연결 경로도 동일한 CORS 정책 적용 ← 추가된 부분
    source.registerCorsConfiguration("/api/sse/**", config);
    return source;
}
```

### 프론트엔드: VITE_API_BASE_URL의 `/api` 중복 방지

SSE URL 생성 시 `VITE_API_BASE_URL`이 `/api`로 끝나면 `/api/api/sse/connect`가 되는 문제도 함께 해결.

**`notification.js`**

```javascript
// VITE_API_BASE_URL이 '/api'로 끝날 수 있으므로 제거하여 중복 방지
let baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
if (baseUrl.endsWith('/api')) {
    baseUrl = baseUrl.slice(0, -4)  // '/api' 제거 → 'http://localhost:8080'
}
const sseUrl = `${baseUrl}/api/sse/connect?token=${token}`
```

---

## CORS 동작 흐름 (수정 후)

```
EventSource → GET http://localhost:8080/api/sse/connect?token=xxx
  Origin: http://localhost:5173
        ↓
Spring Security CORS Filter
  → /api/sse/** 패턴 매칭 ✅
  → allowedOriginPatterns: ["http://localhost:5173"] 매칭 ✅
  → allowCredentials: true ✅
        ↓
응답 헤더 설정:
  Access-Control-Allow-Origin: http://localhost:5173
  Access-Control-Allow-Credentials: true
  Content-Type: text/event-stream
        ↓
브라우저: CORS 검증 통과 ✅ → SSE 연결 수립
```

---

## 설계 판단

### `allowedOriginPatterns` vs `allowedOrigins`

| 설정 | 와일드카드 | Credentials |
|------|----------|-------------|
| `setAllowedOrigins(List.of("*"))` | ✅ 가능 | ❌ `allowCredentials(true)`와 충돌 |
| `setAllowedOriginPatterns(List.of("*"))` | ✅ 가능 | ✅ 함께 사용 가능 |

`allowCredentials(true)`가 필요한 SSE 환경에서는 반드시 `setAllowedOriginPatterns()`를 사용해야 함.
`setAllowedOrigins("*")`와 `setAllowCredentials(true)`를 동시에 설정하면 Spring이 예외를 던짐.

---

## 관련 파일

- `map-log-backend/src/main/java/com/maplog/common/config/SecurityConfig.java`
- `map-log-frontend/src/app/stores/notification.js`

# SSE 연결 시 JWT 인증 실패 (EventSource 헤더 제한)

## 증상

프론트엔드에서 SSE 연결(`EventSource`)을 시도하면 `401 Unauthorized` 에러가 발생하여 실시간 알림 수신이 불가능했음.

```
GET http://localhost:8080/api/sse/connect 401 (Unauthorized)
```

브라우저 콘솔:

```
[SSE] 연결 끊김, 3초 후 재연결...
[SSE] 연결 끊김, 3초 후 재연결...   ← 무한 재연결 루프
```

---

## 원인 분석

### 에러 연쇄 흐름

```
프론트엔드: new EventSource('/api/sse/connect')
        ↓
브라우저 EventSource API → HTTP GET 요청 전송
        ↓
⚠️ EventSource는 커스텀 HTTP 헤더를 설정할 수 없음
   → Authorization: Bearer xxx 헤더가 전달되지 않음
        ↓
JwtAuthenticationFilter.extractToken()
   → request.getHeader("Authorization") = null
   → 토큰 없음 → SecurityContext 미설정
        ↓
Spring Security: anyRequest().authenticated()
   → 인증 정보 없음 → 401 Unauthorized
```

### 핵심 원인

**브라우저의 `EventSource` API는 HTTP 헤더를 커스터마이징할 수 없는 제약**이 있음.

일반 REST API 호출에서는 Axios 인터셉터가 `Authorization: Bearer xxx` 헤더를 자동 첨부하지만,
`EventSource`는 브라우저 네이티브 API로서 헤더 설정 기능을 제공하지 않음.

```javascript
// ✅ Axios - 헤더 설정 가능
axios.get('/api/data', {
    headers: { Authorization: `Bearer ${token}` }
})

// ❌ EventSource - 헤더 설정 불가능
const es = new EventSource('/api/sse/connect')  // 헤더를 넣을 방법이 없음
```

---

## 해결 방법

### 1. 프론트엔드: JWT 토큰을 쿼리 파라미터로 전달

**`notification.js` (Pinia Store)**

```javascript
function connectSSE() {
    const token = localStorage.getItem('ml_access_token')
    if (!token) return

    // 헤더 대신 쿼리 파라미터(?token=xxx)로 JWT 전달
    const sseUrl = `${baseUrl}/api/sse/connect?token=${token}`
    eventSource = new EventSource(sseUrl)
}
```

### 2. 백엔드: JwtAuthenticationFilter에 쿼리 파라미터 토큰 추출 로직 추가

**`JwtAuthenticationFilter.java`**

```java
private String extractToken(HttpServletRequest request) {
    // 1. Authorization 헤더에서 토큰 추출 (일반 API 요청)
    String header = request.getHeader("Authorization");
    if (header != null && header.startsWith("Bearer ")) {
        return header.substring(7);
    }

    // 2. 쿼리 파라미터에서 토큰 추출 (SSE 연결용) ← 추가된 부분
    // EventSource API는 HTTP 헤더를 설정할 수 없으므로
    // ?token=xxx 방식으로 JWT를 전달합니다.
    String queryToken = request.getParameter("token");
    if (queryToken != null && !queryToken.isBlank()) {
        return queryToken;
    }

    return null;
}
```

### 인증 흐름 (수정 후)

```
프론트엔드: new EventSource('/api/sse/connect?token=eyJhbGci...')
        ↓
JwtAuthenticationFilter.extractToken()
   → request.getHeader("Authorization") = null
   → request.getParameter("token") = "eyJhbGci..."  ← 쿼리 파라미터에서 추출
        ↓
jwtTokenProvider.validateToken(token) → 유효
   → SecurityContext에 인증 정보 설정
        ↓
SseController.connect() → @AuthenticationPrincipal로 사용자 정보 접근 가능
   → sseEmitterService.connect(userId) → SSE 연결 성공
```

---

## 보안 고려사항

쿼리 파라미터에 JWT를 노출하면 **서버 액세스 로그나 브라우저 히스토리에 토큰이 기록**될 수 있음.
프로덕션 환경에서는 아래 대안을 검토:

| 대안 | 설명 |
|------|------|
| `fetch()` + `ReadableStream` | fetch API로 SSE를 수동 파싱하면 헤더 설정 가능 |
| 라이브러리 사용 | `eventsource` npm 패키지는 커스텀 헤더 지원 |
| 쿠키 기반 인증 | HttpOnly 쿠키로 토큰 전달 (CSRF 보호 필요) |

현재 프로젝트에서는 **학습 목적 + 내부 서비스** 특성상 쿼리 파라미터 방식을 채택함.

---

## 관련 파일

- `map-log-backend/src/main/java/com/maplog/common/jwt/JwtAuthenticationFilter.java`
- `map-log-frontend/src/app/stores/notification.js`
- `map-log-backend/src/main/java/com/maplog/sse/SseController.java`

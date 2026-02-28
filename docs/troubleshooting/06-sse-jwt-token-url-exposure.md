# SSE 연결 시 JWT 토큰 URL 쿼리 파라미터 노출

## 증상

SSE 연결 URL이 `http://localhost:8080/api/sse/connect?token=eyJhbGci...` 형태로 노출되어 JWT 액세스 토큰이 평문으로 기록됨.

**관찰 사항:**
- 브라우저 개발자 도구 "네트워크" 탭에 전체 토큰이 URL에 표시됨
- 서버 액세스 로그 (nginx, Spring Boot)에 쿼리 문자열 포함 기록
- 리버스 프록시(Nginx) 로그에도 토큰이 평문으로 남음
- CDN 로그에 토큰이 유지되어 보안 침해 위험 증가
- 브라우저 히스토리에도 토큰이 남을 수 있음

---

## 원인 분석

### 에러 연쇄 흐름

```
EventSource Web API 제약
  → 커스텀 HTTP 헤더(Authorization 등) 설정 불가능
      ↓
토큰 전달 방법으로 쿼리 파라미터 선택
      ↓
notification.js:60
  const sseUrl = `${baseUrl}/api/sse/connect?token=${token}`
  new EventSource(sseUrl)
      ↓
JwtAuthenticationFilter.java:66
  String token = request.getParameter("token")
  // 쿼리 파라미터에서 JWT 추출
      ↓
SseController.java
  // SSE 스트림 시작
      ↓
토큰이 URL에 포함되므로 HTTP 요청 로그에 평문으로 기록됨
      ↓
서버 로그, 리버스 프록시 로그, CDN 로그, Referer 헤더에 노출
```

### 핵심 원인

**원인 1. EventSource의 HTTP 헤더 제약**

Web API의 `EventSource`는 보안상의 이유로 커스텀 HTTP 헤더를 설정할 수 없음.
```javascript
// 불가능한 코드
const eventSource = new EventSource(url, {
    headers: { Authorization: `Bearer ${token}` }  // ← EventSource는 이를 지원하지 않음
})
```

**원인 2. 쿼리 파라미터의 로그 기록 특성**

HTTP 메서드(GET, POST)와 다르게, 쿼리 파라미터는 URL의 일부로 취급되어 모든 로그 레이어에 기록됨:
- Nginx 액세스 로그: `GET /api/sse/connect?token=eyJh... HTTP/1.1`
- Spring Boot 로그: `Securing GET /api/sse/connect?token=eyJh...`
- 브라우저 히스토리, 리버스 프록시, CDN, WAF 로그

**원인 3. 기존 구현의 한계**

현재 코드에서는 JWT를 쿼리 파라미터로만 전달할 수 있도록 설계:
```javascript
// map-log-frontend/src/app/stores/notification.js:60
const sseUrl = `${baseUrl}/api/sse/connect?token=${token}`
eventSource = new EventSource(sseUrl)
```

```java
// map-log-backend/src/main/java/com/maplog/common/jwt/JwtAuthenticationFilter.java:66
String token = request.getParameter("token")
```

---

## 해결 방법

`EventSource` 대신 `fetch()` + `ReadableStream`을 사용하여 `Authorization: Bearer {token}` 헤더로 안전하게 전달.

### 변경 전 (취약한 코드)

**`map-log-frontend/src/app/stores/notification.js`**

```javascript
let eventSource = null
let reconnectTimer = null

function connectSSE() {
    if (eventSource) return
    const token = localStorage.getItem('ml_access_token')
    const sseUrl = `${baseUrl}/api/sse/connect?token=${token}`
    eventSource = new EventSource(sseUrl)

    eventSource.addEventListener('connect', (e) => {
        console.log('SSE connected')
    })

    eventSource.addEventListener('notification', (e) => {
        const notification = JSON.parse(e.data)
        state.notifications.unshift(notification)
    })

    eventSource.onerror = () => {
        disconnectSSE()
        reconnectTimer = setTimeout(() => { connectSSE() }, 3000)
    }
}

function disconnectSSE() {
    if (eventSource) {
        eventSource.close()
        eventSource = null
    }
    if (reconnectTimer) {
        clearTimeout(reconnectTimer)
        reconnectTimer = null
    }
}
```

### 변경 후 (안전한 코드)

**`map-log-frontend/src/app/stores/notification.js`**

```javascript
let abortController = null
let reconnectTimer = null

function connectSSE() {
    if (abortController) return
    const token = localStorage.getItem('ml_access_token')
    abortController = new AbortController()

    fetch(`${baseUrl}/api/sse/connect`, {
        headers: {
            Authorization: `Bearer ${token}`,
            Accept: 'text/event-stream'
        },
        signal: abortController.signal,
    })
    .then(response => {
        if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`)
        const reader = response.body.getReader()
        const decoder = new TextDecoder()
        let buffer = ''
        let eventType = ''
        let eventData = ''

        // SSE 스트림 수동 파싱
        const parseStream = async () => {
            try {
                while (true) {
                    const { done, value } = await reader.read()
                    if (done) break

                    buffer += decoder.decode(value, { stream: true })
                    const lines = buffer.split('\n')
                    buffer = lines[lines.length - 1]

                    for (let i = 0; i < lines.length - 1; i++) {
                        const line = lines[i]

                        if (line === '') {
                            // 빈 줄 = 이벤트 완성 신호
                            if (eventType && eventData) {
                                handleSSEEvent(eventType, eventData)
                            }
                            eventType = ''
                            eventData = ''
                        } else if (line.startsWith('event:')) {
                            eventType = line.slice(6).trim()
                        } else if (line.startsWith('data:')) {
                            const data = line.slice(5).trim()
                            eventData += (eventData ? '\n' : '') + data
                        }
                    }
                }
            } catch (error) {
                if (error.name !== 'AbortError') {
                    console.error('SSE stream error:', error)
                    disconnectSSE()
                    reconnectTimer = setTimeout(() => { connectSSE() }, 3000)
                }
            }
        }

        parseStream()
    })
    .catch(error => {
        if (error.name !== 'AbortError') {
            console.error('SSE connection error:', error)
            disconnectSSE()
            reconnectTimer = setTimeout(() => { connectSSE() }, 3000)
        }
    })
}

function handleSSEEvent(eventType, eventData) {
    if (eventType === 'connect') {
        console.log('SSE connected')
    } else if (eventType === 'notification') {
        try {
            const notification = JSON.parse(eventData)
            state.notifications.unshift(notification)
        } catch (e) {
            console.error('Failed to parse notification:', e)
        }
    }
}

function disconnectSSE() {
    if (abortController) {
        abortController.abort()
        abortController = null
    }
    if (reconnectTimer) {
        clearTimeout(reconnectTimer)
        reconnectTimer = null
    }
}
```

### SSE 스트림 파싱 방식

SSE(Server-Sent Events)의 포맷:
```
event: connect\n
data: {"status":"connected"}\n
\n
event: notification\n
data: {"id":1,"message":"..."}\n
\n
```

파싱 로직:
1. 바이트 스트림을 `TextDecoder`로 문자열로 변환
2. `\n`으로 라인 분리
3. `event:` 라인 → 이벤트 타입 저장
4. `data:` 라인 → 데이터 누적 (여러 줄 가능)
5. 빈 줄(`''`) → 이벤트 완성 신호, `handleSSEEvent()` 호출

### 백엔드 수정 (선택사항)

백엔드(`JwtAuthenticationFilter.java`)는 이미 Authorization 헤더를 1순위로 처리하므로 **변경 불필요**:

```java
// 이미 구현된 코드 (src/main/java/com/maplog/common/jwt/JwtAuthenticationFilter.java)
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) {
        // 1순위: Authorization 헤더에서 토큰 추출
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            // JWT 검증 후 SecurityContext에 설정
        }

        // 2순위(fallback): 쿼리 파라미터에서 토큰 추출 (기존 EventSource 호환성)
        if (noTokenYet) {
            String token = request.getParameter("token");
            // JWT 검증 후 SecurityContext에 설정
        }
    }
}
```

쿼리 파라미터 방식이 제거되지 않으므로, 기존 EventSource 클라이언트와 새로운 fetch 클라이언트가 모두 작동함.

---

## 개선 효과

| 항목 | 변경 전 (EventSource) | 변경 후 (fetch + ReadableStream) |
|---|---|---|
| 토큰 전달 방식 | 쿼리 파라미터 (URL에 노출) | Authorization 헤더 (HTTP 헤더) |
| 로그 기록 | 토큰이 URL에 포함되어 모든 로그에 기록됨 | 쿼리 문자열 없음, 헤더는 보통 로깅되지 않음 |
| 브라우저 히스토리 | 토큰 노출 위험 | 안전함 |
| CDN/WAF 로그 | 토큰이 기록됨 | 안전함 |
| HTTP 호환성 | 제한적 (EventSource만 가능) | 표준 fetch, 향후 HTTP/2 Push 마이그레이션 용이 |

---

## 관련 파일

- `map-log-frontend/src/app/stores/notification.js`
- `map-log-backend/src/main/java/com/maplog/common/jwt/JwtAuthenticationFilter.java`
- `map-log-backend/src/main/java/com/maplog/sse/SseController.java`

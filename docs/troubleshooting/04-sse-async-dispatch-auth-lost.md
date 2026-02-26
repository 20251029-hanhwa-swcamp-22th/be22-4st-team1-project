# SSE Async Dispatch 시 인증 정보 소실

## 증상

SSE 연결 자체는 성공하지만(초기 `connect` 이벤트 수신됨), 이후 서버에서 이벤트를 전송하면 `403 Forbidden` 또는 연결이 예고 없이 끊어지는 현상 발생.

```
[SSE] 연결 성공: SSE 연결 성공 - userId: 5     ← 초기 이벤트는 정상
[SSE] 연결 끊김, 3초 후 재연결...               ← 이벤트 전송 시 끊김
```

백엔드 로그:

```
[SSE] 연결 생성 - userId: 5, 현재 연결 수: 1
SecurityContextHolder: context is empty   ← 인증 정보 소실
```

---

## 원인 분석

### SSE와 Async Dispatch의 관계

Spring의 `SseEmitter`는 내부적으로 **비동기 처리(Async Dispatch)** 를 사용함.
초기 요청은 일반 Servlet 스레드에서 처리되지만, 이후 이벤트 전송은 **다른 스레드에서 비동기 디스패치**됨.

```
[요청 1] Servlet Thread → JwtAuthenticationFilter 통과 ✅ → SseController.connect()
                                                              ↓ SseEmitter 반환
[이벤트 전송] Async Thread → 비동기 디스패치 발생
                              ↓
                         JwtAuthenticationFilter.shouldNotFilterAsyncDispatch()
                              ↓
                         기본값: return true (= async dispatch에서 필터 건너뜀)
                              ↓
                         SecurityContext가 설정되지 않음 → 인증 정보 소실
```

### 핵심 원인

`OncePerRequestFilter`의 `shouldNotFilterAsyncDispatch()` 메서드는 **기본값이 `true`** 임.

즉, 비동기 디스패치가 발생하면 해당 필터를 **건너뛰어** JWT 인증이 적용되지 않음.
SSE는 비동기 디스패치를 반복적으로 발생시키므로, 인증 컨텍스트가 유실됨.

```java
// OncePerRequestFilter 기본 구현
protected boolean shouldNotFilterAsyncDispatch() {
    return true;  // async dispatch에서 이 필터를 실행하지 않음
}
```

---

## 해결 방법

### `JwtAuthenticationFilter`에서 `shouldNotFilterAsyncDispatch()` 오버라이드

**`JwtAuthenticationFilter.java`**

```java
@Override
protected boolean shouldNotFilterAsyncDispatch() {
    // SSE는 async dispatch가 발생하므로,
    // 재디스패치에서도 JWT 인증을 유지해야 한다.
    return false;  // ← false로 변경하여 async dispatch에서도 필터 실행
}
```

### 동작 흐름 (수정 후)

```
[요청 1] Servlet Thread → JwtAuthenticationFilter 통과 ✅ → SseController.connect()
                                                              ↓ SseEmitter 반환
[이벤트 전송] Async Thread → 비동기 디스패치 발생
                              ↓
                         shouldNotFilterAsyncDispatch() = false
                              ↓
                         JwtAuthenticationFilter 재실행 ✅
                              ↓
                         쿼리 파라미터에서 토큰 추출 → SecurityContext 재설정
                              ↓
                         emitter.send() 정상 동작 ✅
```

---

## 설계 판단

### 왜 `shouldNotFilterAsyncDispatch()`의 기본값이 `true`인가?

대부분의 웹 요청은 동기 처리이므로 async dispatch에서 필터를 다시 실행하면 **불필요한 오버헤드**가 발생함.
하지만 SSE처럼 비동기 디스패치가 핵심인 기능에서는 `false`로 설정하여 인증 컨텍스트를 유지해야 함.

| 설정 | 동작 | 적합한 경우 |
|------|------|------------|
| `return true` (기본값) | async dispatch에서 필터 건너뜀 | 일반 REST API |
| `return false` | async dispatch에서도 필터 실행 | SSE, 비동기 Servlet |

---

## 관련 파일

- `map-log-backend/src/main/java/com/maplog/common/jwt/JwtAuthenticationFilter.java`
- `map-log-backend/src/main/java/com/maplog/sse/SseEmitterService.java`

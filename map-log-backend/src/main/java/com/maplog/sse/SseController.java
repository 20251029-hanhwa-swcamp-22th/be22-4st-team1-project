package com.maplog.sse;

import com.maplog.common.exception.BusinessException;
import com.maplog.common.exception.ErrorCode;
import com.maplog.user.command.domain.User;
import com.maplog.user.command.repository.UserCommandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SseController - SSE 연결 엔드포인트
 *
 * 【역할】
 * 클라이언트가 GET /api/sse/connect 로 접속하면
 * SseEmitter 스트림을 반환하여 실시간 이벤트 수신을 시작합니다.
 *
 * 【인증】
 * JWT 토큰이 필요합니다. (SecurityConfig의 anyRequest().authenticated()에 해당)
 *
 * 【프론트엔드 연결 예시】
 * const eventSource = new EventSource('/api/sse/connect?token=...')
 * eventSource.addEventListener('notification', (e) => { ... })
 */
@RestController
@RequestMapping("/api/sse")
@RequiredArgsConstructor
public class SseController {

    private final SseEmitterService sseEmitterService;
    private final UserCommandRepository userCommandRepository;

    /**
     * SSE 연결 엔드포인트
     *
     * produces = TEXT_EVENT_STREAM_VALUE:
     * Content-Type을 text/event-stream으로 설정하여
     * 브라우저가 SSE 프로토콜로 인식하게 합니다.
     */
    @GetMapping(value = "/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userCommandRepository.findByEmailAndDeletedAtIsNull(userDetails.getUsername())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return sseEmitterService.connect(user.getId());
    }
}

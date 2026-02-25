package com.maplog.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SseEmitterService - SSE(Server-Sent Events) 연결 관리 서비스
 *
 * 【역할】
 * 사용자별 SseEmitter 인스턴스를 관리합니다.
 * 알림이 발생하면 해당 사용자의 SseEmitter를 통해 실시간으로 이벤트를 전송합니다.
 *
 * 【구조】
 * - ConcurrentHashMap으로 userId → SseEmitter 매핑 (스레드 세이프)
 * - 타임아웃(30분)이나 에러 발생 시 자동 정리
 */
@Slf4j
@Service
public class SseEmitterService {

    // 사용자별 SSE 연결을 저장하는 맵 (스레드 세이프)
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    // SSE 연결 타임아웃: 30분 (밀리초)
    private static final Long TIMEOUT = 30 * 60 * 1000L;

    /**
     * 【SSE 연결 생성】
     * 사용자가 SSE 엔드포인트에 접속하면 호출됩니다.
     * 새 SseEmitter를 생성하고, 기존 연결이 있으면 교체합니다.
     *
     * @param userId 연결할 사용자 ID
     * @return 생성된 SseEmitter
     */
    public SseEmitter connect(Long userId) {
        // 기존 연결이 있으면 먼저 정리
        SseEmitter existing = emitters.get(userId);
        if (existing != null) {
            existing.complete();
            emitters.remove(userId);
        }

        SseEmitter emitter = new SseEmitter(TIMEOUT);

        // 타임아웃 시 맵에서 제거
        emitter.onTimeout(() -> {
            log.info("[SSE] 타임아웃 - userId: {}", userId);
            emitters.remove(userId);
        });

        // 에러 발생 시 맵에서 제거
        emitter.onError((e) -> {
            log.warn("[SSE] 에러 발생 - userId: {}, error: {}", userId, e.getMessage());
            emitters.remove(userId);
        });

        // 연결 완료(클라이언트 disconnect) 시 맵에서 제거
        emitter.onCompletion(() -> {
            log.info("[SSE] 연결 종료 - userId: {}", userId);
            emitters.remove(userId);
        });

        emitters.put(userId, emitter);

        // 연결 직후 더미 이벤트 전송 (연결 확인 + 일부 프록시의 버퍼링 방지)
        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("SSE 연결 성공 - userId: " + userId));
        } catch (IOException e) {
            log.error("[SSE] 초기 이벤트 전송 실패 - userId: {}", userId);
            emitters.remove(userId);
        }

        log.info("[SSE] 연결 생성 - userId: {}, 현재 연결 수: {}", userId, emitters.size());
        return emitter;
    }

    /**
     * 【이벤트 전송】
     * 특정 사용자에게 SSE 이벤트를 전송합니다.
     * 해당 사용자가 연결되어 있지 않으면 무시됩니다.
     *
     * @param userId    수신할 사용자 ID
     * @param eventName 이벤트 이름 (예: "notification", "friend-request")
     * @param data      전송할 데이터 (JSON 직렬화됨)
     */
    public void send(Long userId, String eventName, Object data) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter == null) {
            // 해당 사용자가 SSE에 연결되어 있지 않으면 무시 (오프라인 상태)
            return;
        }

        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(data));
            log.debug("[SSE] 이벤트 전송 성공 - userId: {}, event: {}", userId, eventName);
        } catch (IOException e) {
            log.warn("[SSE] 이벤트 전송 실패 - userId: {}, 연결 제거", userId);
            emitters.remove(userId);
        }
    }
}

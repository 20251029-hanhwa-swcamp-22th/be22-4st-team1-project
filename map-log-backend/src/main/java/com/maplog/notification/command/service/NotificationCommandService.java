package com.maplog.notification.command.service;

import com.maplog.common.exception.BusinessException;
import com.maplog.common.exception.ErrorCode;
import com.maplog.notification.command.domain.Notification;
import com.maplog.notification.command.domain.NotificationType;
import com.maplog.notification.command.repository.NotificationCommandRepository;
import com.maplog.sse.SseEmitterService;
import com.maplog.user.command.domain.User;
import com.maplog.user.command.repository.UserCommandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationCommandService {

    private final NotificationCommandRepository notificationCommandRepository;
    private final UserCommandRepository userCommandRepository;
    private final SseEmitterService sseEmitterService;

    public void createFriendRequestNotification(Long receiverId, Long friendId, String requesterNickname) {
        Notification notification = Notification.create(
                receiverId,
                NotificationType.FRIEND_REQUEST,
                friendId,
                String.format("'%s'님으로부터 새로운 친구 요청이 도착했습니다.", requesterNickname));
        notificationCommandRepository.save(notification);
        // 【SSE 실시간 푸시】친구 요청 알림을 수신자에게 즉시 전송
        sseEmitterService.send(receiverId, "notification",
                Map.of("type", "FRIEND_REQUEST", "message", notification.getMessage()));
    }

    public void createFriendAcceptedNotification(Long requesterId, Long friendId, String receiverNickname) {
        Notification notification = Notification.create(
                requesterId,
                NotificationType.FRIEND_ACCEPTED,
                friendId,
                String.format("'%s'님이 친구 요청을 수락했습니다.", receiverNickname));
        notificationCommandRepository.save(notification);
        // 【SSE 실시간 푸시】친구 수락 알림을 요청자에게 즉시 전송
        sseEmitterService.send(requesterId, "notification",
                Map.of("type", "FRIEND_ACCEPTED", "message", notification.getMessage()));
    }

    public void createDiarySharedNotification(Long receiverId, Long diaryId, String diaryTitle, String sharerNickname) {
        Notification notification = Notification.create(
                receiverId,
                NotificationType.DIARY_SHARED,
                diaryId,
                String.format("'%s'님이 '%s' 일기를 공유했습니다.", sharerNickname, diaryTitle));
        notificationCommandRepository.save(notification);
        // 【SSE 실시간 푸시】일기 공유 알림을 수신자에게 즉시 전송
        sseEmitterService.send(receiverId, "notification",
                Map.of("type", "DIARY_SHARED", "message", notification.getMessage()));
    }

    public void markAsRead(String email, Long notificationId) {
        User user = getUser(email);
        Notification notification = notificationCommandRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));

        if (!notification.getUserId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        notification.markAsRead();
    }

    public void markAllAsRead(String email) {
        User user = getUser(email);
        notificationCommandRepository.markAllAsRead(user.getId());
    }

    public void deleteAll(String email, Boolean readFilter) {
        User user = getUser(email);
        if (readFilter == null) {
            notificationCommandRepository.deleteAllByUserId(user.getId());
        } else {
            notificationCommandRepository.deleteByUserIdAndRead(user.getId(), readFilter);
        }
    }

    private User getUser(String email) {
        return userCommandRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
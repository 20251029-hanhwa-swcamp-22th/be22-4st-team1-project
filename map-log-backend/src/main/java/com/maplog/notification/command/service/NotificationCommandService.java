/**
 * 시스템 내에서 발생하는 다양한 이벤트를 사용자 알림으로 변환하여 저장하고 관리하는 서비스입니다.
 */
package com.maplog.notification.command.service;

import com.maplog.common.exception.BusinessException;
import com.maplog.common.exception.ErrorCode;
import com.maplog.notification.command.domain.Notification;
import com.maplog.notification.command.domain.NotificationType;
import com.maplog.notification.command.repository.NotificationCommandRepository;
import com.maplog.user.command.domain.User;
import com.maplog.user.command.repository.UserCommandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationCommandService {

    private final NotificationCommandRepository notificationCommandRepository;
    private final UserCommandRepository userCommandRepository;

    /** 친구 요청 수신 시 알림 생성 */
    public void createFriendRequestNotification(Long receiverId, Long friendId, String requesterNickname) {
        Notification notification = Notification.create(
                receiverId,
                NotificationType.FRIEND_REQUEST,
                friendId,
                String.format("'%s'님으로부터 새로운 친구 요청이 도착했습니다.", requesterNickname)
        );
        notificationCommandRepository.save(notification);
    }

    /** 친구 요청이 수락되었을 때 요청자에게 알림 생성 */
    public void createFriendAcceptedNotification(Long requesterId, Long friendId, String receiverNickname) {
        Notification notification = Notification.create(
                requesterId,
                NotificationType.FRIEND_ACCEPTED,
                friendId,
                String.format("'%s'님이 친구 요청을 수락했습니다.", receiverNickname)
        );
        notificationCommandRepository.save(notification);
    }

    /** 일기가 특정 사용자에게 공유되었을 때 알림 생성 */
    public void createDiarySharedNotification(Long receiverId, Long diaryId, String diaryTitle, String sharerNickname) {
        Notification notification = Notification.create(
                receiverId,
                NotificationType.DIARY_SHARED,
                diaryId,
                String.format("'%s'님이 '%s' 일기를 공유했습니다.", sharerNickname, diaryTitle)
        );
        notificationCommandRepository.save(notification);
    }

    /** 단일 알림을 읽음 처리 */
    public void markAsRead(String email, Long notificationId) {
        User user = getUser(email);
        Notification notification = notificationCommandRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));

        // 보안 확인: 본인의 알림만 읽음 처리 가능
        if (!notification.getUserId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        notification.markAsRead();
    }

    /** 사용자의 모든 알림을 한꺼번에 읽음 처리 (JPQL 벌크 연산 활용) */
    public void markAllAsRead(String email) {
        User user = getUser(email);
        notificationCommandRepository.markAllAsRead(user.getId());
    }

    /** 사용자의 알림을 선택적으로 혹은 일괄 삭제 */
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

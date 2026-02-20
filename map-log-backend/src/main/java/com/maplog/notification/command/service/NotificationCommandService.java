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

    public void createFriendRequestNotification(Long receiverId, Long friendId) {
        Notification notification = Notification.create(
                receiverId,
                NotificationType.FRIEND_REQUEST,
                friendId,
                "새로운 친구 요청이 도착했습니다."
        );
        notificationCommandRepository.save(notification);
    }

    public void createFriendAcceptedNotification(Long requesterId, Long friendId) {
        Notification notification = Notification.create(
                requesterId,
                NotificationType.FRIEND_ACCEPTED,
                friendId,
                "친구 요청이 수락되었습니다."
        );
        notificationCommandRepository.save(notification);
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
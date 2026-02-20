package com.maplog.notification.query.service;

import com.maplog.common.exception.BusinessException;
import com.maplog.common.exception.ErrorCode;
import com.maplog.notification.command.repository.NotificationCommandRepository;
import com.maplog.notification.query.dto.NotificationResponse;
import com.maplog.user.command.domain.User;
import com.maplog.user.command.repository.UserCommandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationQueryService {

    private final NotificationCommandRepository notificationCommandRepository;
    private final UserCommandRepository userCommandRepository;

    public Page<NotificationResponse> getNotifications(String email, Pageable pageable) {
        User user = getUser(email);
        return notificationCommandRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable)
                .map(n -> new NotificationResponse(
                        n.getId(), n.getType().name(), n.getReferenceId(),
                        n.getMessage(), n.isRead(), n.getCreatedAt()
                ));
    }

    private User getUser(String email) {
        return userCommandRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
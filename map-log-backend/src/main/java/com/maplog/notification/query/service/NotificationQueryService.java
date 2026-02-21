package com.maplog.notification.query.service;

import com.maplog.common.exception.BusinessException;
import com.maplog.common.exception.ErrorCode;
import com.maplog.notification.query.dto.NotificationResponse;
import com.maplog.notification.query.mapper.NotificationQueryMapper;
import com.maplog.user.command.domain.User;
import com.maplog.user.command.repository.UserCommandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationQueryService {

    private final UserCommandRepository userCommandRepository;
    private final NotificationQueryMapper notificationQueryMapper;

    public Page<NotificationResponse> getNotifications(String email, Boolean readFilter, Pageable pageable) {
        User user = getUser(email);
        int offset = (int) pageable.getOffset();
        int size = pageable.getPageSize();
        List<NotificationResponse> items = notificationQueryMapper.findNotifications(user.getId(), readFilter, offset, size);
        long total = notificationQueryMapper.countNotifications(user.getId(), readFilter);
        return new PageImpl<>(items, pageable, total);
    }

    private User getUser(String email) {
        return userCommandRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
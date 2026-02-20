package com.maplog.user.command.service;

import com.maplog.common.exception.BusinessException;
import com.maplog.common.exception.ErrorCode;
import com.maplog.user.command.domain.User;
import com.maplog.user.command.dto.UserStatusUpdateRequest;
import com.maplog.user.command.repository.UserCommandRepository;
import com.maplog.user.query.dto.AdminUserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminUserService {

    private final UserCommandRepository userCommandRepository;

    @Transactional(readOnly = true)
    public Page<AdminUserResponse> getUsers(Pageable pageable) {
        return userCommandRepository.findAllByDeletedAtIsNull(pageable)
                .map(u -> new AdminUserResponse(
                        u.getId(),
                        u.getEmail(),
                        u.getNickname(),
                        u.getRole().name(),
                        u.getStatus().name(),
                        u.getCreatedAt()
                ));
    }

    public void changeUserStatus(Long userId, UserStatusUpdateRequest request) {
        User user = userCommandRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        user.changeStatus(request.status());
    }
}
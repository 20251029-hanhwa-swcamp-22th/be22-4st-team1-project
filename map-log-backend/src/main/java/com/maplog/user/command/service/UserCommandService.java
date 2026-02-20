package com.maplog.user.command.service;

import com.maplog.common.exception.BusinessException;
import com.maplog.common.exception.ErrorCode;
import com.maplog.user.command.domain.User;
import com.maplog.user.command.dto.UpdateProfileRequest;
import com.maplog.user.command.repository.RefreshTokenRepository;
import com.maplog.user.command.repository.UserCommandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserCommandService {

    private final UserCommandRepository userCommandRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public void updateProfile(String email, UpdateProfileRequest request) {
        User user = getUserByEmail(email);
        user.updateProfile(request.nickname(), request.profileImageUrl());
    }

    public void deleteAccount(String email) {
        User user = getUserByEmail(email);
        refreshTokenRepository.deleteByUserId(user.getId());
        user.softDelete();
    }

    private User getUserByEmail(String email) {
        return userCommandRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
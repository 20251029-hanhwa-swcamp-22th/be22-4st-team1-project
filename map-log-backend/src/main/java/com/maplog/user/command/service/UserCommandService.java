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

    public void updateProfile(String email, String nickname, String profileImageUrl) {
        User user = getUserByEmail(email);
        if (nickname != null && !nickname.equals(user.getNickname())
                && userCommandRepository.existsByNicknameAndIdNotAndDeletedAtIsNull(nickname, user.getId())) {
            throw new BusinessException(ErrorCode.NICKNAME_ALREADY_EXISTS);
        }
        user.updateProfile(nickname, profileImageUrl);
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
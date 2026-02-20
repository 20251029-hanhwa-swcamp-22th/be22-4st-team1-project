package com.maplog.user.query.service;

import com.maplog.common.exception.BusinessException;
import com.maplog.common.exception.ErrorCode;
import com.maplog.user.command.domain.User;
import com.maplog.user.command.repository.UserCommandRepository;
import com.maplog.user.query.dto.UserProfileQueryResponse;
import com.maplog.user.query.dto.UserSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserQueryService {

    private final UserCommandRepository userCommandRepository;

    public UserProfileQueryResponse getMyProfile(String email) {
        User user = userCommandRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return toProfileResponse(user);
    }

    public boolean isNicknameAvailable(String nickname) {
        return !userCommandRepository.existsByNicknameAndDeletedAtIsNull(nickname);
    }

    public List<UserSummaryResponse> searchUsers(String keyword) {
        return userCommandRepository.findByNicknameContainingAndDeletedAtIsNull(keyword)
                .stream()
                .map(u -> new UserSummaryResponse(u.getId(), u.getNickname(), u.getProfileImageUrl()))
                .toList();
    }

    private UserProfileQueryResponse toProfileResponse(User user) {
        return new UserProfileQueryResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getProfileImageUrl(),
                user.getRole().name(),
                user.getCreatedAt()
        );
    }
}
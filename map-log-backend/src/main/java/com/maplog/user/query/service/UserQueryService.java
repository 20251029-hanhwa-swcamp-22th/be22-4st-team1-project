package com.maplog.user.query.service;

import com.maplog.common.exception.BusinessException;
import com.maplog.common.exception.ErrorCode;
import com.maplog.user.command.repository.UserCommandRepository;
import com.maplog.user.query.dto.UserProfileQueryResponse;
import com.maplog.user.query.dto.UserSummaryResponse;
import com.maplog.user.query.mapper.UserQueryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserQueryService {

    private final UserCommandRepository userCommandRepository;
    private final UserQueryMapper userQueryMapper;

    public UserProfileQueryResponse getMyProfile(String email) {
        UserProfileQueryResponse response = userQueryMapper.findMyProfile(email);
        if (response == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return response;
    }

    public boolean isNicknameAvailable(String nickname) {
        return !userCommandRepository.existsByNicknameAndDeletedAtIsNull(nickname);
    }

    public List<UserSummaryResponse> searchUsers(String keyword) {
        return userQueryMapper.searchUsers(keyword);
    }
}
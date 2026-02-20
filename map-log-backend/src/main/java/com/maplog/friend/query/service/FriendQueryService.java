package com.maplog.friend.query.service;

import com.maplog.common.exception.BusinessException;
import com.maplog.common.exception.ErrorCode;
import com.maplog.diary.query.dto.DiarySummaryResponse;
import com.maplog.friend.query.dto.FriendRequestResponse;
import com.maplog.friend.query.dto.FriendSummaryResponse;
import com.maplog.friend.query.mapper.FriendQueryMapper;
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
public class FriendQueryService {

    private final UserCommandRepository userCommandRepository;
    private final FriendQueryMapper friendQueryMapper;

    public List<FriendSummaryResponse> getFriends(String email) {
        User user = getUser(email);
        return friendQueryMapper.findFriends(user.getId());
    }

    public Page<FriendRequestResponse> getPendingRequests(String email, Pageable pageable) {
        User user = getUser(email);
        int offset = (int) pageable.getOffset();
        int size = pageable.getPageSize();
        List<FriendRequestResponse> items = friendQueryMapper.findPendingRequests(user.getId(), offset, size);
        long total = friendQueryMapper.countPendingRequests(user.getId());
        return new PageImpl<>(items, pageable, total);
    }

    public Page<DiarySummaryResponse> getFeed(String email, Pageable pageable) {
        User user = getUser(email);
        int offset = (int) pageable.getOffset();
        int size = pageable.getPageSize();
        List<DiarySummaryResponse> items = friendQueryMapper.findFeed(user.getId(), offset, size);
        long total = friendQueryMapper.countFeed(user.getId());
        return new PageImpl<>(items, pageable, total);
    }

    private User getUser(String email) {
        return userCommandRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
package com.maplog.friend.query.service;

import com.maplog.common.exception.BusinessException;
import com.maplog.common.exception.ErrorCode;
import com.maplog.diary.command.repository.DiaryCommandRepository;
import com.maplog.diary.query.dto.DiarySummaryResponse;
import com.maplog.friend.command.domain.Friend;
import com.maplog.friend.command.domain.FriendStatus;
import com.maplog.friend.command.repository.FriendCommandRepository;
import com.maplog.friend.query.dto.FriendRequestResponse;
import com.maplog.friend.query.dto.FriendSummaryResponse;
import com.maplog.user.command.domain.User;
import com.maplog.user.command.repository.UserCommandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FriendQueryService {

    private final FriendCommandRepository friendCommandRepository;
    private final UserCommandRepository userCommandRepository;
    private final DiaryCommandRepository diaryCommandRepository;

    public List<FriendSummaryResponse> getFriends(String email) {
        User user = getUser(email);
        return friendCommandRepository.findAcceptedFriends(user.getId())
                .stream()
                .map(f -> {
                    Long friendUserId = f.getRequesterId().equals(user.getId())
                            ? f.getReceiverId() : f.getRequesterId();
                    User friendUser = userCommandRepository.findById(friendUserId)
                            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
                    return new FriendSummaryResponse(
                            f.getId(), friendUser.getId(),
                            friendUser.getNickname(), friendUser.getProfileImageUrl(),
                            f.getUpdatedAt()
                    );
                })
                .toList();
    }

    public Page<FriendRequestResponse> getPendingRequests(String email, Pageable pageable) {
        User user = getUser(email);
        return friendCommandRepository.findByReceiverIdAndStatus(user.getId(), FriendStatus.PENDING, pageable)
                .map(f -> {
                    User requester = userCommandRepository.findById(f.getRequesterId())
                            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
                    return new FriendRequestResponse(
                            f.getId(), requester.getId(),
                            requester.getNickname(), requester.getProfileImageUrl(),
                            f.getCreatedAt()
                    );
                });
    }

    public Page<DiarySummaryResponse> getFeed(String email, Pageable pageable) {
        User user = getUser(email);
        List<Friend> friends = friendCommandRepository.findAcceptedFriends(user.getId());

        if (friends.isEmpty()) {
            return Page.empty(pageable);
        }

        Set<Long> friendIds = friends.stream()
                .map(f -> f.getRequesterId().equals(user.getId()) ? f.getReceiverId() : f.getRequesterId())
                .collect(Collectors.toSet());

        return diaryCommandRepository.findFriendFeed(friendIds, pageable)
                .map(d -> new DiarySummaryResponse(
                        d.getId(), d.getTitle(), d.getLocationName(),
                        d.getVisitedAt(), d.getVisibility().name(), d.getCreatedAt()
                ));
    }

    private User getUser(String email) {
        return userCommandRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
package com.maplog.friend.command.service;

import com.maplog.common.exception.BusinessException;
import com.maplog.common.exception.ErrorCode;
import com.maplog.friend.command.domain.Friend;
import com.maplog.friend.command.domain.FriendStatus;
import com.maplog.friend.command.dto.FriendRespondRequest;
import com.maplog.friend.command.dto.SendFriendRequest;
import com.maplog.friend.command.repository.FriendCommandRepository;
import com.maplog.notification.command.service.NotificationCommandService;
import com.maplog.user.command.domain.User;
import com.maplog.user.command.repository.UserCommandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class FriendCommandService {

    private final FriendCommandRepository friendCommandRepository;
    private final UserCommandRepository userCommandRepository;
    private final NotificationCommandService notificationCommandService;

    public void sendFriendRequest(String email, SendFriendRequest request) {
        User requester = getUser(email);

        if (requester.getId().equals(request.receiverId())) {
            throw new BusinessException(ErrorCode.FRIEND_REQUEST_SELF);
        }

        userCommandRepository.findByIdAndDeletedAtIsNull(request.receiverId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        friendCommandRepository.findByUsers(requester.getId(), request.receiverId())
                .ifPresent(f -> {
                    if (f.isPending())
                        throw new BusinessException(ErrorCode.ALREADY_FRIEND_REQUESTED);
                    if (f.getStatus() == FriendStatus.ACCEPTED)
                        throw new BusinessException(ErrorCode.ALREADY_FRIEND);
                });

        Friend friend = friendCommandRepository.save(Friend.create(requester.getId(), request.receiverId()));
        notificationCommandService.createFriendRequestNotification(request.receiverId(), friend.getId(),
                requester.getNickname());
    }

    public void respondToRequest(String email, Long friendId, FriendRespondRequest request) {
        User receiver = getUser(email);
        Friend friend = friendCommandRepository.findById(friendId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FRIEND_REQUEST_NOT_FOUND));

        if (!friend.getReceiverId().equals(receiver.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        if (!friend.isPending()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }

        if (request.status() == FriendStatus.ACCEPTED) {
            friend.accept();
            notificationCommandService.createFriendAcceptedNotification(friend.getRequesterId(), friend.getId(),
                    receiver.getNickname());
        } else if (request.status() == FriendStatus.REJECTED) {
            friend.reject();
        } else {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
    }

    /**
     * 친구 삭제(끊기)
     * - friendId로 친구 관계를 조회
     * - 요청한 사용자가 해당 관계의 당사자(요청자 또는 수신자)인지 검증
     * - 검증 통과 시 친구 관계 레코드를 DB에서 삭제
     */
    public void deleteFriend(String email, Long friendId) {
        User user = getUser(email);
        Friend friend = friendCommandRepository.findById(friendId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FRIEND_REQUEST_NOT_FOUND));

        // 요청자 또는 수신자 중 한 명이어야 삭제 가능
        if (!friend.getRequesterId().equals(user.getId()) && !friend.getReceiverId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        friendCommandRepository.delete(friend);
    }

    private User getUser(String email) {
        return userCommandRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
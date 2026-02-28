package com.maplog.friend.command.service;

import com.maplog.common.exception.BusinessException;
import com.maplog.common.exception.ErrorCode;
import com.maplog.friend.command.domain.Friend;
import com.maplog.friend.command.domain.FriendStatus;
import com.maplog.friend.command.dto.FriendRespondRequest;
import com.maplog.friend.command.dto.SendFriendRequest;
import com.maplog.friend.command.repository.FriendCommandRepository;
import com.maplog.notification.command.service.NotificationCommandService;
import com.maplog.sse.SseEmitterService;
import com.maplog.user.command.domain.User;
import com.maplog.user.command.repository.UserCommandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class FriendCommandService {

    private final FriendCommandRepository friendCommandRepository;
    private final UserCommandRepository userCommandRepository;
    private final NotificationCommandService notificationCommandService;
    private final SseEmitterService sseEmitterService;

    public void sendFriendRequest(String email, SendFriendRequest request) {
        User requester = getUser(email);
        Long requesterId = requester.getId();
        Long receiverId = request.receiverId();

        if (requesterId.equals(receiverId)) {
            throw new BusinessException(ErrorCode.FRIEND_REQUEST_SELF);
        }

        userCommandRepository.findByIdAndDeletedAtIsNull(receiverId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Optional<Friend> existingFriendOpt = friendCommandRepository.findByUsers(requesterId, receiverId);
        if (existingFriendOpt.isPresent()) {
            Friend existingFriend = existingFriendOpt.get();
            if (existingFriend.isPending()) {
                throw new BusinessException(ErrorCode.ALREADY_FRIEND_REQUESTED);
            }
            if (existingFriend.getStatus() == FriendStatus.ACCEPTED) {
                throw new BusinessException(ErrorCode.ALREADY_FRIEND);
            }

            // REJECTED 상태인 기존 친구 요청을 재활성화하여 중복 insert를 방지한다.
            existingFriend.reRequest(requesterId, receiverId);
            notificationCommandService.createFriendRequestNotification(receiverId, existingFriend.getId(),
                    requester.getNickname());
            return;
        }

        Friend friend = friendCommandRepository.save(Friend.create(requesterId, receiverId));
        notificationCommandService.createFriendRequestNotification(receiverId, friend.getId(),
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

            // 【SSE 직접 전송】요청자(A)에게 친구 수락 이벤트 → A의 친구 목록 실시간 갱신
            sseEmitterService.send(friend.getRequesterId(), "notification",
                    Map.of("type", "FRIEND_ACCEPTED", "message",
                            String.format("'%s'님이 친구 요청을 수락했습니다.", receiver.getNickname())));
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

        // 【SSE 실시간 푸시】상대방에게 친구 삭제 이벤트 전송
        Long otherUserId = friend.getRequesterId().equals(user.getId())
                ? friend.getReceiverId()
                : friend.getRequesterId();
        sseEmitterService.send(otherUserId, "notification",
                Map.of("type", "FRIEND_DELETED", "message", "친구가 삭제되었습니다."));
    }

    private User getUser(String email) {
        return userCommandRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}

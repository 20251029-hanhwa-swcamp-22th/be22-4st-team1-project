/**
 * 사용자 간의 친구 요청, 수락, 거절 등 관계 관리 로직을 담당하는 서비스입니다.
 */
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

    /**
     * 특정 사용자에게 친구 요청을 보냅니다.
     * @param email 요청을 보내는 사용자(본인)의 이메일
     * @param request 상대방의 ID를 포함한 DTO
     * @throws BusinessException 본인에게 요청하거나 이미 관계가 존재하는 경우 발생
     */
    public void sendFriendRequest(String email, SendFriendRequest request) {
        User requester = getUser(email);

        // 비즈니스 제약: 자기 자신에게는 친구 요청 불가
        if (requester.getId().equals(request.receiverId())) {
            throw new BusinessException(ErrorCode.FRIEND_REQUEST_SELF);
        }

        userCommandRepository.findByIdAndDeletedAtIsNull(request.receiverId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 이미 요청 중이거나 친구 상태인지 확인
        friendCommandRepository.findByUsers(requester.getId(), request.receiverId())
                .ifPresent(f -> {
                    if (f.isPending()) throw new BusinessException(ErrorCode.ALREADY_FRIEND_REQUESTED);
                    if (f.getStatus() == FriendStatus.ACCEPTED) throw new BusinessException(ErrorCode.ALREADY_FRIEND);
                });

        // 친구 정보 저장 및 상대방에게 알림 발송
        Friend friend = friendCommandRepository.save(Friend.create(requester.getId(), request.receiverId()));
        notificationCommandService.createFriendRequestNotification(
                request.receiverId(), friend.getId(), requester.getNickname());
    }

    /**
     * 받은 친구 요청에 대해 수락 또는 거절 응답을 합니다.
     * @param email 응답하는 사용자(수신자)의 이메일
     * @param friendId 처리할 친구 관계 PK
     * @param request ACCEPTED 혹은 REJECTED 상태값
     */
    public void respondToRequest(String email, Long friendId, FriendRespondRequest request) {
        User receiver = getUser(email);
        Friend friend = friendCommandRepository.findById(friendId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FRIEND_REQUEST_NOT_FOUND));

        // 권한 확인: 본인에게 온 요청만 처리 가능
        if (!friend.getReceiverId().equals(receiver.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 이미 처리된 요청은 다시 수정 불가
        if (!friend.isPending()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }

        if (request.status() == FriendStatus.ACCEPTED) {
            friend.accept();
            // 수락 시 요청자에게 '수락 완료' 알림 발송
            notificationCommandService.createFriendAcceptedNotification(
                    friend.getRequesterId(), friend.getId(), receiver.getNickname());
        } else if (request.status() == FriendStatus.REJECTED) {
            friend.reject();
        } else {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
    }

    private User getUser(String email) {
        return userCommandRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}

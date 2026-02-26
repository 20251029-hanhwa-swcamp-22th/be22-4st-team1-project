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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FriendCommandServiceTest {

    @InjectMocks
    private FriendCommandService friendCommandService;

    @Mock
    private FriendCommandRepository friendCommandRepository;

    @Mock
    private UserCommandRepository userCommandRepository;

    @Mock
    private NotificationCommandService notificationCommandService;

    @Mock
    private SseEmitterService sseEmitterService;

    @Nested
    @DisplayName("친구 요청 테스트")
    class SendFriendRequestTest {
        @Test
        @DisplayName("성공")
        void success() {
            // given
            String email = "requester@email.com";
            SendFriendRequest request = new SendFriendRequest(2L);
            User requester = User.create(email, "pw", "reqNickname");
            ReflectionTestUtils.setField(requester, "id", 1L);
            User receiver = User.create("receiver@email.com", "pw", "rec");
            ReflectionTestUtils.setField(receiver, "id", 2L);

            given(userCommandRepository.findByEmailAndDeletedAtIsNull(email)).willReturn(Optional.of(requester));
            given(userCommandRepository.findByIdAndDeletedAtIsNull(2L)).willReturn(Optional.of(receiver));
            given(friendCommandRepository.findByUsers(1L, 2L)).willReturn(Optional.empty());
            
            Friend savedFriend = Friend.create(1L, 2L);
            ReflectionTestUtils.setField(savedFriend, "id", 100L);
            given(friendCommandRepository.save(any(Friend.class))).willReturn(savedFriend);

            // when
            friendCommandService.sendFriendRequest(email, request);

            // then
            verify(friendCommandRepository).save(any(Friend.class));
            verify(notificationCommandService).createFriendRequestNotification(2L, 100L, "reqNickname");
        }

        @Test
        @DisplayName("자기 자신에게 요청 시 예외 발생")
        void failSelfRequest() {
            // given
            String email = "requester@email.com";
            SendFriendRequest request = new SendFriendRequest(1L);
            User requester = User.create(email, "pw", "req");
            ReflectionTestUtils.setField(requester, "id", 1L);

            given(userCommandRepository.findByEmailAndDeletedAtIsNull(email)).willReturn(Optional.of(requester));

            // when & then
            assertThatThrownBy(() -> friendCommandService.sendFriendRequest(email, request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FRIEND_REQUEST_SELF);
        }

        @Test
        @DisplayName("이미 친구인 경우 예외 발생")
        void failAlreadyFriend() {
            // given
            String email = "requester@email.com";
            SendFriendRequest request = new SendFriendRequest(2L);
            User requester = User.create(email, "pw", "req");
            ReflectionTestUtils.setField(requester, "id", 1L);
            
            Friend friend = Friend.create(1L, 2L);
            friend.accept();

            given(userCommandRepository.findByEmailAndDeletedAtIsNull(email)).willReturn(Optional.of(requester));
            given(userCommandRepository.findByIdAndDeletedAtIsNull(2L)).willReturn(Optional.of(mock(User.class)));
            given(friendCommandRepository.findByUsers(1L, 2L)).willReturn(Optional.of(friend));

            // when & then
            assertThatThrownBy(() -> friendCommandService.sendFriendRequest(email, request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ALREADY_FRIEND);
        }

        @Test
        @DisplayName("이미 친구 신청을 보낸 경우 예외 발생")
        void failAlreadyRequested() {
            // given
            String email = "requester@email.com";
            SendFriendRequest request = new SendFriendRequest(2L);
            User requester = User.create(email, "pw", "req");
            ReflectionTestUtils.setField(requester, "id", 1L);

            Friend friend = Friend.create(1L, 2L); // Default status is PENDING

            given(userCommandRepository.findByEmailAndDeletedAtIsNull(email)).willReturn(Optional.of(requester));
            given(userCommandRepository.findByIdAndDeletedAtIsNull(2L)).willReturn(Optional.of(mock(User.class)));
            given(friendCommandRepository.findByUsers(1L, 2L)).willReturn(Optional.of(friend));

            // when & then
            assertThatThrownBy(() -> friendCommandService.sendFriendRequest(email, request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ALREADY_FRIEND_REQUESTED);
        }
    }

    @Nested
    @DisplayName("친구 응답 테스트")
    class RespondToRequestTest {
        @Test
        @DisplayName("성공 - 수락")
        void successAccept() {
            // given
            String email = "receiver@email.com";
            User receiver = User.create(email, "pw", "recNickname");
            ReflectionTestUtils.setField(receiver, "id", 2L);
            
            Friend friend = Friend.create(1L, 2L);
            ReflectionTestUtils.setField(friend, "id", 100L);

            given(userCommandRepository.findByEmailAndDeletedAtIsNull(email)).willReturn(Optional.of(receiver));
            given(friendCommandRepository.findById(100L)).willReturn(Optional.of(friend));

            // when
            friendCommandService.respondToRequest(email, 100L, new FriendRespondRequest(FriendStatus.ACCEPTED));

            // then
            verify(notificationCommandService).createFriendAcceptedNotification(1L, 100L, "recNickname");
        }

        @Test
        @DisplayName("권한 없는 사용자가 응답 시 예외 발생")
        void failForbidden() {
            // given
            String email = "other@email.com";
            User other = User.create(email, "pw", "other");
            ReflectionTestUtils.setField(other, "id", 3L);
            
            Friend friend = Friend.create(1L, 2L);
            ReflectionTestUtils.setField(friend, "id", 100L);

            given(userCommandRepository.findByEmailAndDeletedAtIsNull(email)).willReturn(Optional.of(other));
            given(friendCommandRepository.findById(100L)).willReturn(Optional.of(friend));

            // when & then
            assertThatThrownBy(() -> friendCommandService.respondToRequest(email, 100L, new FriendRespondRequest(FriendStatus.ACCEPTED)))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
        }

        @Test
        @DisplayName("존재하지 않는 친구 요청에 응답 시도 시 예외 발생")
        void failRequestNotFound() {
            // given
            String email = "receiver@email.com";
            User receiver = User.create(email, "pw", "recNickname");
            given(userCommandRepository.findByEmailAndDeletedAtIsNull(email)).willReturn(Optional.of(receiver));
            given(friendCommandRepository.findById(anyLong())).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> friendCommandService.respondToRequest(email, 999L, new FriendRespondRequest(FriendStatus.ACCEPTED)))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FRIEND_REQUEST_NOT_FOUND);
        }
    }
}

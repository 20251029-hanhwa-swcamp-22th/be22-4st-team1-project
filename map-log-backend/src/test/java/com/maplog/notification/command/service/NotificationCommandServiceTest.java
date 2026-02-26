package com.maplog.notification.command.service;

import com.maplog.common.exception.BusinessException;
import com.maplog.common.exception.ErrorCode;
import com.maplog.notification.command.domain.Notification;
import com.maplog.notification.command.domain.NotificationType;
import com.maplog.notification.command.repository.NotificationCommandRepository;
import com.maplog.sse.SseEmitterService;
import com.maplog.user.command.domain.User;
import com.maplog.user.command.repository.UserCommandRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationCommandServiceTest {

    @InjectMocks
    private NotificationCommandService notificationCommandService;

    @Mock
    private NotificationCommandRepository notificationCommandRepository;

    @Mock
    private UserCommandRepository userCommandRepository;

    @Mock
    private SseEmitterService sseEmitterService;

    @Nested
    @DisplayName("create notification tests")
    class CreateNotificationTest {

        @Test
        @DisplayName("friend request notification is saved and pushed by SSE")
        void createFriendRequestNotificationSuccess() {
            notificationCommandService.createFriendRequestNotification(2L, 100L, "alice");

            ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationCommandRepository).save(notificationCaptor.capture());

            Notification saved = notificationCaptor.getValue();
            assertThat(saved.getUserId()).isEqualTo(2L);
            assertThat(saved.getType()).isEqualTo(NotificationType.FRIEND_REQUEST);
            assertThat(saved.getReferenceId()).isEqualTo(100L);
            assertThat(saved.isRead()).isFalse();
            assertThat(saved.getMessage()).contains("alice");

            ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
            verify(sseEmitterService).send(eq(2L), eq("notification"), payloadCaptor.capture());

            assertThat(payloadCaptor.getValue()).isInstanceOf(Map.class);
            Map<?, ?> payload = (Map<?, ?>) payloadCaptor.getValue();
            assertThat(payload.get("type")).isEqualTo("FRIEND_REQUEST");
            assertThat(String.valueOf(payload.get("message"))).contains("alice");
        }

        @Test
        @DisplayName("friend accepted notification is saved and pushed by SSE")
        void createFriendAcceptedNotificationSuccess() {
            notificationCommandService.createFriendAcceptedNotification(1L, 200L, "bob");

            ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationCommandRepository).save(notificationCaptor.capture());

            Notification saved = notificationCaptor.getValue();
            assertThat(saved.getUserId()).isEqualTo(1L);
            assertThat(saved.getType()).isEqualTo(NotificationType.FRIEND_ACCEPTED);
            assertThat(saved.getReferenceId()).isEqualTo(200L);
            assertThat(saved.getMessage()).contains("bob");

            ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
            verify(sseEmitterService).send(eq(1L), eq("notification"), payloadCaptor.capture());

            Map<?, ?> payload = (Map<?, ?>) payloadCaptor.getValue();
            assertThat(payload.get("type")).isEqualTo("FRIEND_ACCEPTED");
            assertThat(String.valueOf(payload.get("message"))).contains("bob");
        }

        @Test
        @DisplayName("diary shared notification is saved and pushed by SSE")
        void createDiarySharedNotificationSuccess() {
            notificationCommandService.createDiarySharedNotification(3L, 300L, "trip", "carol");

            ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationCommandRepository).save(notificationCaptor.capture());

            Notification saved = notificationCaptor.getValue();
            assertThat(saved.getUserId()).isEqualTo(3L);
            assertThat(saved.getType()).isEqualTo(NotificationType.DIARY_SHARED);
            assertThat(saved.getReferenceId()).isEqualTo(300L);
            assertThat(saved.getMessage()).contains("trip", "carol");

            ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
            verify(sseEmitterService).send(eq(3L), eq("notification"), payloadCaptor.capture());

            Map<?, ?> payload = (Map<?, ?>) payloadCaptor.getValue();
            assertThat(payload.get("type")).isEqualTo("DIARY_SHARED");
            assertThat(String.valueOf(payload.get("message"))).contains("trip", "carol");
        }
    }

    @Nested
    @DisplayName("mark as read tests")
    class MarkAsReadTest {

        @Test
        @DisplayName("owner can mark notification as read")
        void markAsReadSuccess() {
            String email = "user@test.com";
            User user = createUser(1L, email);
            Notification notification = Notification.create(1L, NotificationType.FRIEND_REQUEST, 10L, "msg");

            given(userCommandRepository.findByEmailAndDeletedAtIsNull(email)).willReturn(Optional.of(user));
            given(notificationCommandRepository.findById(10L)).willReturn(Optional.of(notification));

            notificationCommandService.markAsRead(email, 10L);

            assertThat(notification.isRead()).isTrue();
        }

        @Test
        @DisplayName("throws when notification does not exist")
        void markAsReadFailNotFound() {
            String email = "user@test.com";
            User user = createUser(1L, email);

            given(userCommandRepository.findByEmailAndDeletedAtIsNull(email)).willReturn(Optional.of(user));
            given(notificationCommandRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> notificationCommandService.markAsRead(email, 999L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOTIFICATION_NOT_FOUND);
        }

        @Test
        @DisplayName("throws when non-owner tries to mark notification")
        void markAsReadFailForbidden() {
            String email = "user@test.com";
            User user = createUser(1L, email);
            Notification notification = Notification.create(2L, NotificationType.DIARY_SHARED, 11L, "msg");

            given(userCommandRepository.findByEmailAndDeletedAtIsNull(email)).willReturn(Optional.of(user));
            given(notificationCommandRepository.findById(11L)).willReturn(Optional.of(notification));

            assertThatThrownBy(() -> notificationCommandService.markAsRead(email, 11L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
        }
    }

    @Nested
    @DisplayName("bulk operation tests")
    class BulkOperationTest {

        @Test
        @DisplayName("markAllAsRead delegates to repository")
        void markAllAsReadSuccess() {
            String email = "user@test.com";
            User user = createUser(5L, email);
            given(userCommandRepository.findByEmailAndDeletedAtIsNull(email)).willReturn(Optional.of(user));

            notificationCommandService.markAllAsRead(email);

            verify(notificationCommandRepository).markAllAsRead(5L);
        }

        @Test
        @DisplayName("deleteAll with null filter deletes all notifications")
        void deleteAllSuccessWithoutFilter() {
            String email = "user@test.com";
            User user = createUser(6L, email);
            given(userCommandRepository.findByEmailAndDeletedAtIsNull(email)).willReturn(Optional.of(user));

            notificationCommandService.deleteAll(email, null);

            verify(notificationCommandRepository).deleteAllByUserId(6L);
            verify(notificationCommandRepository, never()).deleteByUserIdAndRead(anyLong(), anyBoolean());
        }

        @Test
        @DisplayName("deleteAll with read filter delegates to filtered delete")
        void deleteAllSuccessWithFilter() {
            String email = "user@test.com";
            User user = createUser(7L, email);
            given(userCommandRepository.findByEmailAndDeletedAtIsNull(email)).willReturn(Optional.of(user));

            notificationCommandService.deleteAll(email, true);

            verify(notificationCommandRepository).deleteByUserIdAndRead(7L, true);
            verify(notificationCommandRepository, never()).deleteAllByUserId(anyLong());
        }

        @Test
        @DisplayName("throws when user does not exist")
        void markAllAsReadFailUserNotFound() {
            given(userCommandRepository.findByEmailAndDeletedAtIsNull("none@test.com")).willReturn(Optional.empty());

            assertThatThrownBy(() -> notificationCommandService.markAllAsRead("none@test.com"))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
        }
    }

    private User createUser(Long id, String email) {
        User user = User.create(email, "encoded", "nick");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}

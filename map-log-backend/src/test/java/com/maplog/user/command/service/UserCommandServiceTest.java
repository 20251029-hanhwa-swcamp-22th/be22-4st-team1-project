package com.maplog.user.command.service;

import com.maplog.common.exception.BusinessException;
import com.maplog.common.exception.ErrorCode;
import com.maplog.user.command.domain.User;
import com.maplog.user.command.repository.RefreshTokenRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserCommandServiceTest {

    @InjectMocks
    private UserCommandService userCommandService;

    @Mock
    private UserCommandRepository userCommandRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Nested
    @DisplayName("updateProfile tests")
    class UpdateProfileTest {

        @Test
        @DisplayName("updates nickname and profile image")
        void updateProfileSuccess() {
            String email = "user@test.com";
            User user = createUser(1L, email, "oldNick");
            given(userCommandRepository.findByEmailAndDeletedAtIsNull(email)).willReturn(Optional.of(user));
            given(userCommandRepository.existsByNicknameAndIdNotAndDeletedAtIsNull("newNick", 1L)).willReturn(false);

            userCommandService.updateProfile(email, "newNick", "/uploads/new.png");

            assertThat(user.getNickname()).isEqualTo("newNick");
            assertThat(user.getProfileImageUrl()).isEqualTo("/uploads/new.png");
            verify(userCommandRepository).existsByNicknameAndIdNotAndDeletedAtIsNull("newNick", 1L);
        }

        @Test
        @DisplayName("skips duplicate nickname check when nickname is unchanged")
        void updateProfileSkipDuplicateCheckWhenSameNickname() {
            String email = "user@test.com";
            User user = createUser(2L, email, "sameNick");
            given(userCommandRepository.findByEmailAndDeletedAtIsNull(email)).willReturn(Optional.of(user));

            userCommandService.updateProfile(email, "sameNick", null);

            assertThat(user.getNickname()).isEqualTo("sameNick");
            verify(userCommandRepository, never()).existsByNicknameAndIdNotAndDeletedAtIsNull("sameNick", 2L);
        }

        @Test
        @DisplayName("throws when nickname already exists")
        void updateProfileFailDuplicateNickname() {
            String email = "user@test.com";
            User user = createUser(3L, email, "oldNick");
            given(userCommandRepository.findByEmailAndDeletedAtIsNull(email)).willReturn(Optional.of(user));
            given(userCommandRepository.existsByNicknameAndIdNotAndDeletedAtIsNull("takenNick", 3L)).willReturn(true);

            assertThatThrownBy(() -> userCommandService.updateProfile(email, "takenNick", null))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NICKNAME_ALREADY_EXISTS);
        }

        @Test
        @DisplayName("throws when user does not exist")
        void updateProfileFailUserNotFound() {
            given(userCommandRepository.findByEmailAndDeletedAtIsNull("none@test.com")).willReturn(Optional.empty());

            assertThatThrownBy(() -> userCommandService.updateProfile("none@test.com", "nick", null))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("deleteAccount tests")
    class DeleteAccountTest {

        @Test
        @DisplayName("deletes refresh token and soft deletes user")
        void deleteAccountSuccess() {
            String email = "user@test.com";
            User user = createUser(10L, email, "nick");
            given(userCommandRepository.findByEmailAndDeletedAtIsNull(email)).willReturn(Optional.of(user));

            userCommandService.deleteAccount(email);

            verify(refreshTokenRepository).deleteByUserId(10L);
            assertThat(user.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("throws when user does not exist")
        void deleteAccountFailUserNotFound() {
            given(userCommandRepository.findByEmailAndDeletedAtIsNull("none@test.com")).willReturn(Optional.empty());

            assertThatThrownBy(() -> userCommandService.deleteAccount("none@test.com"))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
            verify(refreshTokenRepository, never()).deleteByUserId(org.mockito.ArgumentMatchers.anyLong());
        }
    }

    private User createUser(Long id, String email, String nickname) {
        User user = User.create(email, "encoded", nickname);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}

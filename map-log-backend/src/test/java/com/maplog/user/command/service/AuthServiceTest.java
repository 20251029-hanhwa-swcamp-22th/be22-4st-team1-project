package com.maplog.user.command.service;

import com.maplog.common.exception.BusinessException;
import com.maplog.common.exception.ErrorCode;
import com.maplog.common.jwt.JwtTokenProvider;
import com.maplog.user.command.domain.RefreshToken;
import com.maplog.user.command.domain.User;
import com.maplog.user.command.domain.UserStatus;
import com.maplog.user.command.dto.LoginRequest;
import com.maplog.user.command.dto.LoginResponse;
import com.maplog.user.command.dto.SignupRequest;
import com.maplog.user.command.repository.RefreshTokenRepository;
import com.maplog.user.command.repository.UserCommandRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private UserCommandRepository userCommandRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshTokenExpiration", 3600000L);
    }

    @Nested
    @DisplayName("회원가입 테스트")
    class SignupTest {
        @Test
        @DisplayName("성공")
        void success() {
            // given
            SignupRequest request = new SignupRequest("test@email.com", "password123", "nickname");
            given(userCommandRepository.existsByEmailAndDeletedAtIsNull(anyString())).willReturn(false);
            given(userCommandRepository.existsByNicknameAndDeletedAtIsNull(anyString())).willReturn(false);
            given(passwordEncoder.encode(anyString())).willReturn("encodedPassword");

            // when
            authService.signup(request);

            // then
            verify(userCommandRepository, times(1)).save(any(User.class));
        }

        @Test
        @DisplayName("이메일 중복 시 예외 발생")
        void failDuplicateEmail() {
            // given
            SignupRequest request = new SignupRequest("test@email.com", "password123", "nickname");
            given(userCommandRepository.existsByEmailAndDeletedAtIsNull(anyString())).willReturn(true);

            // when & then
            assertThatThrownBy(() -> authService.signup(request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        @Test
        @DisplayName("닉네임 중복 시 예외 발생")
        void failDuplicateNickname() {
            // given
            SignupRequest request = new SignupRequest("test@email.com", "password123", "nickname");
            given(userCommandRepository.existsByEmailAndDeletedAtIsNull(anyString())).willReturn(false);
            given(userCommandRepository.existsByNicknameAndDeletedAtIsNull(anyString())).willReturn(true);

            // when & then
            assertThatThrownBy(() -> authService.signup(request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NICKNAME_ALREADY_EXISTS);
        }
    }

    @Nested
    @DisplayName("로그인 테스트")
    class LoginTest {
        @Test
        @DisplayName("성공")
        void success() {
            // given
            LoginRequest request = new LoginRequest("test@email.com", "password123");
            User user = User.create("test@email.com", "encodedPassword", "nickname");
            ReflectionTestUtils.setField(user, "id", 1L);

            given(userCommandRepository.findByEmailAndDeletedAtIsNull(request.email()))
                    .willReturn(Optional.of(user));
            given(passwordEncoder.matches(request.password(), user.getPassword()))
                    .willReturn(true);
            given(jwtTokenProvider.generateAccessToken(user.getEmail())).willReturn("accessToken");
            given(jwtTokenProvider.generateRefreshToken(user.getEmail())).willReturn("refreshToken");

            // when
            LoginResponse response = authService.login(request);

            // then
            assertThat(response.accessToken()).isEqualTo("accessToken");
            assertThat(response.refreshToken()).isEqualTo("refreshToken");
            verify(refreshTokenRepository).deleteByUserId(user.getId());
            verify(refreshTokenRepository).save(any(RefreshToken.class));
        }

        @Test
        @DisplayName("비밀번호 불일치 시 예외 발생")
        void failInvalidPassword() {
            // given
            LoginRequest request = new LoginRequest("test@email.com", "wrongPassword");
            User user = User.create("test@email.com", "encodedPassword", "nickname");

            given(userCommandRepository.findByEmailAndDeletedAtIsNull(request.email()))
                    .willReturn(Optional.of(user));
            given(passwordEncoder.matches(request.password(), user.getPassword()))
                    .willReturn(false);

            // when & then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PASSWORD);
        }

        @Test
        @DisplayName("정지된 사용자 로그인 시도 시 예외 발생")
        void failSuspendedUser() {
            // given
            LoginRequest request = new LoginRequest("test@email.com", "password123");
            User user = User.create("test@email.com", "encodedPassword", "nickname");
            user.changeStatus(UserStatus.SUSPENDED, "reason", null);

            given(userCommandRepository.findByEmailAndDeletedAtIsNull(request.email()))
                    .willReturn(Optional.of(user));
            given(passwordEncoder.matches(request.password(), user.getPassword()))
                    .willReturn(true);

            // when & then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
        }

        @Test
        @DisplayName("존재하지 않는 이메일로 로그인 시 예외 발생")
        void failUserNotFound() {
            // given
            LoginRequest request = new LoginRequest("notexist@email.com", "password123");
            given(userCommandRepository.findByEmailAndDeletedAtIsNull(request.email()))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("토큰 재발급 테스트")
    class RefreshTest {

        @Test
        @DisplayName("성공")
        void success() {
            // given
            String tokenValue = "valid-refresh-token";
            RefreshToken refreshToken = RefreshToken.create(1L, tokenValue, LocalDateTime.now().plusDays(1));
            User user = User.create("test@email.com", "encodedPassword", "nickname");
            ReflectionTestUtils.setField(user, "id", 1L);

            given(refreshTokenRepository.findByToken(tokenValue)).willReturn(Optional.of(refreshToken));
            given(jwtTokenProvider.getSubject(tokenValue)).willReturn("test@email.com");
            given(userCommandRepository.findByEmailAndDeletedAtIsNull("test@email.com")).willReturn(Optional.of(user));
            given(jwtTokenProvider.generateAccessToken("test@email.com")).willReturn("newAccessToken");
            given(jwtTokenProvider.generateRefreshToken("test@email.com")).willReturn("newRefreshToken");

            // when
            LoginResponse response = authService.refresh(tokenValue);

            // then
            assertThat(response.accessToken()).isEqualTo("newAccessToken");
            assertThat(response.refreshToken()).isEqualTo("newRefreshToken");
            verify(refreshTokenRepository).delete(refreshToken);
            verify(refreshTokenRepository).save(any(RefreshToken.class));
        }

        @Test
        @DisplayName("존재하지 않는 토큰 시 예외 발생")
        void failInvalidToken() {
            // given
            given(refreshTokenRepository.findByToken(anyString())).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> authService.refresh("invalid-token"))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_TOKEN);
        }

        @Test
        @DisplayName("만료된 토큰 시 예외 발생")
        void failExpiredToken() {
            // given
            String tokenValue = "expired-refresh-token";
            RefreshToken refreshToken = RefreshToken.create(1L, tokenValue, LocalDateTime.now().minusDays(1));
            given(refreshTokenRepository.findByToken(tokenValue)).willReturn(Optional.of(refreshToken));

            // when & then
            assertThatThrownBy(() -> authService.refresh(tokenValue))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXPIRED_TOKEN);
        }
    }

    @Nested
    @DisplayName("로그아웃 테스트")
    class LogoutTest {

        @Test
        @DisplayName("성공")
        void success() {
            // given
            String email = "test@email.com";
            User user = User.create(email, "encodedPassword", "nickname");
            ReflectionTestUtils.setField(user, "id", 1L);
            given(userCommandRepository.findByEmailAndDeletedAtIsNull(email)).willReturn(Optional.of(user));

            // when
            authService.logout(email);

            // then
            verify(refreshTokenRepository).deleteByUserId(1L);
        }
    }
}

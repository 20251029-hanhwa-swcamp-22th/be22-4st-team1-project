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
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserCommandRepository userCommandRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    public void signup(SignupRequest request) {
        if (userCommandRepository.existsByEmailAndDeletedAtIsNull(request.email())) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        User user = User.create(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.nickname()
        );
        userCommandRepository.save(user);
    }

    public LoginResponse login(LoginRequest request) {
        User user = userCommandRepository.findByEmailAndDeletedAtIsNull(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }

        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        String accessToken = jwtTokenProvider.generateAccessToken(user.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());

        refreshTokenRepository.deleteByUserId(user.getId());
        refreshTokenRepository.save(RefreshToken.create(
                user.getId(),
                refreshToken,
                LocalDateTime.now().plusSeconds(refreshTokenExpiration / 1000)
        ));

        return new LoginResponse(accessToken, refreshToken, "Bearer");
    }

    public LoginResponse refresh(String refreshTokenValue) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN));

        if (refreshToken.isExpired()) {
            refreshTokenRepository.delete(refreshToken);
            throw new BusinessException(ErrorCode.EXPIRED_TOKEN);
        }

        jwtTokenProvider.validateToken(refreshTokenValue);
        String subject = jwtTokenProvider.getSubject(refreshTokenValue);

        String newAccessToken = jwtTokenProvider.generateAccessToken(subject);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(subject);

        User user = userCommandRepository.findByEmailAndDeletedAtIsNull(subject)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        refreshTokenRepository.delete(refreshToken);
        refreshTokenRepository.save(RefreshToken.create(
                user.getId(),
                newRefreshToken,
                LocalDateTime.now().plusSeconds(refreshTokenExpiration / 1000)
        ));

        return new LoginResponse(newAccessToken, newRefreshToken, "Bearer");
    }

    public void logout(String email) {
        User user = userCommandRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        refreshTokenRepository.deleteByUserId(user.getId());
    }
}
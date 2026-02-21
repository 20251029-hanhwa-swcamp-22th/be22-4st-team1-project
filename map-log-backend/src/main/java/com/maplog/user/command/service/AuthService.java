/**
 * 회원가입, 로그인, 토큰 재발급 등 인증 전반의 핵심 비즈니스 로직을 수행하는 서비스입니다.
 */
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

    /** 
     * 새로운 사용자를 등록합니다. 
     * 이메일과 닉네임의 중복 여부를 먼저 검증합니다. 
     */
    public void signup(SignupRequest request) {
        if (userCommandRepository.existsByEmailAndDeletedAtIsNull(request.email())) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        if (userCommandRepository.existsByNicknameAndDeletedAtIsNull(request.nickname())) {
            throw new BusinessException(ErrorCode.NICKNAME_ALREADY_EXISTS);
        }
        
        // 비밀번호는 반드시 암호화하여 저장
        User user = User.create(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.nickname()
        );
        userCommandRepository.save(user);
    }

    /**
     * 로그인을 수행하고 Access/Refresh 토큰 한 쌍을 발급합니다.
     * @return 로그인 성공 정보 및 토큰
     */
    public LoginResponse login(LoginRequest request) {
        User user = userCommandRepository.findByEmailAndDeletedAtIsNull(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 비밀번호 일치 확인
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }

        // 계정 정지 여부 확인
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 토큰 생성 및 Refresh Token DB 갱신
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

    /**
     * Refresh Token을 이용해 만료된 Access Token을 재발급합니다.
     * RTR(Refresh Token Rotation) 전략이 적용되어 있어, 재발급 시 Refresh Token도 새로 발급됩니다.
     */
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

        // 보안을 위해 기존 Refresh Token 삭제 후 신규 저장
        refreshTokenRepository.delete(refreshToken);
        refreshTokenRepository.save(RefreshToken.create(
                user.getId(),
                newRefreshToken,
                LocalDateTime.now().plusSeconds(refreshTokenExpiration / 1000)
        ));

        return new LoginResponse(newAccessToken, newRefreshToken, "Bearer");
    }

    /** 사용자 로그아웃 처리 (서버 측 Refresh Token 만료) */
    public void logout(String email) {
        User user = userCommandRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        refreshTokenRepository.deleteByUserId(user.getId());
    }
}

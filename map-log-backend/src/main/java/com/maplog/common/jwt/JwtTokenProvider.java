/**
 * JWT(JSON Web Token)의 생성, 파싱 및 유효성 검증을 담당하는 컴포넌트입니다.
 * 외부 라이브러리: io.jsonwebtoken (jjwt)를 사용하여 보안 토큰을 관리합니다.
 */
package com.maplog.common.jwt;

import com.maplog.common.exception.BusinessException;
import com.maplog.common.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration}") long accessTokenExpiration,
            @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration) {
        // HMAC SHA 알고리즘에 적합한 SecretKey 생성
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    /** Access Token 생성 */
    public String generateAccessToken(String subject) {
        return buildToken(subject, accessTokenExpiration);
    }

    /** Refresh Token 생성 */
    public String generateRefreshToken(String subject) {
        return buildToken(subject, refreshTokenExpiration);
    }

    /** 토큰에서 사용자 식별자(Email) 추출 */
    public String getSubject(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * 토큰의 유효성 및 만료 여부를 확인합니다.
     * @throws BusinessException 토큰이 만료되었거나 형식이 잘못된 경우 발생
     */
    public void validateToken(String token) {
        try {
            parseClaims(token);
        } catch (ExpiredJwtException e) {
            throw new BusinessException(ErrorCode.EXPIRED_TOKEN);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
    }

    /** JWT 빌더를 이용한 실제 토큰 생성 로직 */
    private String buildToken(String subject, long expiration) {
        Date now = new Date();
        return Jwts.builder()
                .subject(subject)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiration))
                .signWith(secretKey)
                .compact();
    }

    /** 토큰 복호화 및 클레임 추출 */
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

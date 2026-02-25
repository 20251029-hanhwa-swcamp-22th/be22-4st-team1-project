package com.maplog.common.jwt;

import com.maplog.common.exception.BusinessException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        // SSE는 async dispatch가 발생하므로, 재디스패치에서도 JWT 인증을 유지해야 한다.
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);

        if (token != null) {
            try {
                jwtTokenProvider.validateToken(token);
                String subject = jwtTokenProvider.getSubject(token);
                UserDetails userDetails = userDetailsService.loadUserByUsername(subject);

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (BusinessException e) {
                // 토큰이 만료/유효하지 않은 경우 SecurityContext를 설정하지 않고 필터 체인을 계속 진행
                // permitAll() 엔드포인트는 정상 동작하고, 인증이 필요한 엔드포인트는 Spring Security가 401 처리
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        // 1. Authorization 헤더에서 토큰 추출 (일반 API 요청)
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }

        // 2. 쿼리 파라미터에서 토큰 추출 (SSE 연결용)
        // EventSource API는 HTTP 헤더를 설정할 수 없으므로
        // ?token=xxx 방식으로 JWT를 전달합니다.
        String queryToken = request.getParameter("token");
        if (queryToken != null && !queryToken.isBlank()) {
            return queryToken;
        }

        return null;
    }

}

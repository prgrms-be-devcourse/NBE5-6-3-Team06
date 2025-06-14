package com.grepp.matnam.infra.auth.jwt;

import com.grepp.matnam.app.model.auth.token.service.RefreshTokenService;
import com.grepp.matnam.infra.auth.CookieUtils;
import com.grepp.matnam.infra.auth.jwt.code.TokenType;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class LogoutFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        return !"/api/auth/logout".equals(path) || !"POST".equals(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String userId = null;

        try {
            String accessToken = jwtProvider.resolveToken(request, TokenType.ACCESS_TOKEN);
            if (StringUtils.hasText(accessToken)) {
                Claims claims = jwtProvider.parseClaim(accessToken);
                if (claims != null) {
                    userId = claims.getSubject();
                    log.debug("Access Token에서 사용자 정보 추출: {}", userId);
                } else {
                    log.warn("Access Token 파싱 실패 - 형식 오류");
                }
            }
        } catch (Exception e) {
            log.warn("로그아웃 중 토큰 처리 오류: {}", e.getMessage());
        }

        if (userId == null) {
            try {
                String refreshToken = jwtProvider.resolveToken(request, TokenType.REFRESH_TOKEN);
                if (StringUtils.hasText(refreshToken)) {
                    var storedRefreshToken = refreshTokenService.findByToken(refreshToken);
                    if (storedRefreshToken != null) {
                        userId = storedRefreshToken.getUserId();
                        log.debug("Refresh Token에서 사용자 정보 추출: {}", userId);
                    }
                }
            } catch (Exception e) {
                log.warn("Refresh Token에서 사용자 정보 추출 실패: {}", e.getMessage());
            }
        }

        if (userId != null) {
            try {
                refreshTokenService.deleteByUserId(userId);
                log.info("로그아웃 처리 완료: {}", userId);
            } catch (Exception e) {
                log.warn("Refresh Token 삭제 실패: {}", e.getMessage());
            }
        } else {
            log.warn("로그아웃 요청이지만 사용자 정보를 찾을 수 없음");
        }

        SecurityContextHolder.clearContext();
        CookieUtils.clearAllCookies(request, response);
        filterChain.doFilter(request, response);
    }
}
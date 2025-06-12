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
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class LogoutFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        if(path.equals("/api/auth/logout")){
            String accessToken = jwtProvider.resolveToken(request, TokenType.ACCESS_TOKEN);

            if(accessToken != null) {
                try {
                    Claims claims = jwtProvider.parseClaim(accessToken);
                    if(claims.getId() != null) {
                        refreshTokenService.deleteByAccessTokenId(claims.getId());
                        log.info("로그아웃 처리 완료: {}", claims.getSubject());
                    }
                } catch (Exception e) {
                    log.warn("로그아웃 중 토큰 처리 오류: {}", e.getMessage());
                }
            }

            CookieUtils.clearAllCookies(request, response);

            response.sendRedirect("/");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
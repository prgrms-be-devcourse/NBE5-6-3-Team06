package com.grepp.matnam.infra.auth.jwt;

import com.grepp.matnam.app.model.auth.service.AuthService;
import com.grepp.matnam.app.model.auth.token.dto.TokenDto;
import com.grepp.matnam.app.model.auth.token.repository.UserBlackListRepository;
import com.grepp.matnam.infra.auth.CookieUtils;
import com.grepp.matnam.infra.auth.jwt.code.TokenType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final UserBlackListRepository userBlackListRepository;
    private final AuthService authService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        List<String> excludePath = new ArrayList<>();
        excludePath.addAll(List.of("/error", "/favicon.ico", "/css", "/img", "/js"));
        excludePath.addAll(List.of("/user/signup", "/user/signin", "/user/oauth2/signup"));
        excludePath.addAll(List.of("/api/auth/signin", "/api/auth/signup", "/api/auth/refresh"));
        excludePath.addAll(List.of("/signin/oauth2/code/", "/swagger-ui", "/v3/api-docs"));
        excludePath.addAll(List.of("/api/ai/chat", "/api/ai/restaurant/name"));
        excludePath.addAll(List.of("/api/test/redis"));
        excludePath.addAll(List.of("/.well-known/"));

        String path = request.getRequestURI();

        return excludePath.stream().anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestAccessToken = jwtProvider.resolveToken(request, TokenType.ACCESS_TOKEN);
        String refreshToken = jwtProvider.resolveToken(request, TokenType.REFRESH_TOKEN);

        log.debug("토큰 상태 - Access Token: {}, Refresh Token: {}",
                requestAccessToken != null ? "존재" : "없음",
                refreshToken != null ? "존재" : "없음");

        boolean tokenRenewed = false;

        // 토큰이 모두 없는 경우
        if (requestAccessToken == null && refreshToken == null) {
            log.debug("토큰이 모두 없음 - 인증 없이 진행");
        }
        // Access Token은 없지만 Refresh Token은 있는 경우 (자동 갱신)
        else if (requestAccessToken == null && refreshToken != null) {
            log.debug("Access Token 없음, Refresh Token으로 자동 갱신 시도");
            tokenRenewed = attemptTokenRenewal(refreshToken, response);
        }
        // Access Token이 있는 경우
        else if (requestAccessToken != null) {
            // 토큰 유효성 검증
            try {
                if (jwtProvider.validateToken(requestAccessToken)) {
                    Claims claims = jwtProvider.parseClaim(requestAccessToken);

                    if (!isBlacklistedUser(claims.getSubject())) {
                        Authentication authentication = jwtProvider.generateAuthentication(requestAccessToken);
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        log.debug("Access Token 인증 성공: {}", claims.getSubject());
                    } else {
                        log.warn("블랙리스트 사용자 접근 시도: {}", claims.getSubject());
                    }
                } else {
                    log.debug("Access Token 검증 실패");
                }
            } catch (ExpiredJwtException ex) {
                log.debug("Access Token 만료, 자동 갱신 시도");
                if (refreshToken != null) {
                    tokenRenewed = attemptTokenRenewal(refreshToken, response);
                }
            } catch (Exception e) {
                log.warn("Access Token 처리 중 오류: {}", e.getMessage());
            }
        }

        if (!response.isCommitted()) {
            Authentication finalAuth = SecurityContextHolder.getContext().getAuthentication();
            if (finalAuth != null) {
                log.debug("SecurityContext - 사용자: {}, 권한: {}, 인증됨: {}",
                        finalAuth.getName(), finalAuth.getAuthorities(), finalAuth.isAuthenticated());
            } else {
                log.debug("SecurityContext에 인증 정보 없음");
            }

            filterChain.doFilter(request, response);
        } else {
            log.warn("응답이 이미 커밋되어 필터 체인 진행 불가");
        }
    }

    private boolean attemptTokenRenewal(String refreshToken, HttpServletResponse response) {
        try {
            if (response.isCommitted()) {
                log.warn("응답이 이미 커밋되어 토큰 갱신 불가");
                return false;
            }

            TokenDto tokenDto = authService.refreshTokenWithValue(refreshToken);

            if (tokenDto != null) {
                if (!response.isCommitted()) {
                    setTokenCookiesSafely(response, tokenDto);

                    Authentication authentication = jwtProvider.generateAuthentication(tokenDto.getAccessToken());
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    log.info("토큰 갱신 및 인증 설정 성공");
                    return true;
                } else {
                    log.warn("토큰 갱신은 성공했지만 응답이 커밋되어 쿠키 설정 불가");
                    Authentication authentication = jwtProvider.generateAuthentication(tokenDto.getAccessToken());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    return true;
                }
            }
        } catch (Exception e) {
            log.warn("토큰 갱신 실패: {}", e.getMessage());
        }
        return false;
    }

    private boolean isBlacklistedUser(String userId) {
        try {
            return userBlackListRepository.existsById(userId);
        } catch (Exception e) {
            log.warn("블랙리스트 확인 실패 (무시하고 진행): {}", e.getMessage());
            return false;
        }
    }

    private void setTokenCookiesSafely(HttpServletResponse response, TokenDto tokenDto) {
        try {
            if (response.isCommitted()) {
                log.warn("응답이 이미 커밋되어 쿠키 설정 불가");
                return;
            }

            CookieUtils.addTokenCookie(response, "ACCESS_TOKEN", tokenDto.getAccessToken(), "/");
            CookieUtils.addTokenCookie(response, "REFRESH_TOKEN", tokenDto.getRefreshToken(), "/");

            log.debug("토큰 쿠키 설정 완료");
        } catch (IllegalStateException e) {
            log.warn("응답 상태로 인한 쿠키 설정 실패: {}", e.getMessage());
        } catch (Exception e) {
            log.error("쿠키 설정 중 예외 발생: {}", e.getMessage());
        }
    }
}
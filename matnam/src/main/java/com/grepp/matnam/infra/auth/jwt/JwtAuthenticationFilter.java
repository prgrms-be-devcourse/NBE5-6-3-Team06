package com.grepp.matnam.infra.auth.jwt;

import com.grepp.matnam.app.model.auth.token.dto.AccessTokenDto;
import com.grepp.matnam.app.model.auth.token.entity.RefreshToken;
import com.grepp.matnam.app.model.auth.token.entity.UserBlackList;
import com.grepp.matnam.app.model.auth.token.repository.UserBlackListRepository;
import com.grepp.matnam.app.model.auth.token.service.RefreshTokenService;
import com.grepp.matnam.infra.auth.CookieUtils;
import com.grepp.matnam.infra.auth.jwt.code.TokenType;
import com.grepp.matnam.infra.error.exceptions.CommonException;
import com.grepp.matnam.infra.response.ResponseCode;
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
    private final RefreshTokenService refreshTokenService;
    private final UserBlackListRepository userBlackListRepository;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        List<String> excludePath = new ArrayList<>();
        excludePath.addAll(List.of("/error", "/favicon.ico", "/css", "/img", "/js"));
        excludePath.addAll(List.of("/user/signup", "/user/signin", "/user/oauth2/signup"));

        excludePath.addAll(List.of("/api/auth/signin", "/api/auth/signup", "/api/auth/refresh"));

        excludePath.addAll(List.of("/signin/oauth2/code/", "/swagger-ui", "/v3/api-docs"));
        excludePath.addAll(List.of("/api/ai/chat", "/api/ai/restaurant/name"));

        excludePath.addAll(List.of("/api/test/redis"));

        String path = request.getRequestURI();
        boolean shouldExclude = excludePath.stream().anyMatch(path::startsWith);

        if (shouldExclude) {
            log.debug("JWT 필터 제외 경로: {}", path);
        } else {
            log.debug("JWT 필터 적용 경로: {}", path);
        }

        return shouldExclude;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestAccessToken = jwtProvider.resolveToken(request, TokenType.ACCESS_TOKEN);
        String refreshToken = jwtProvider.resolveToken(request, TokenType.REFRESH_TOKEN);

        if (requestAccessToken == null) {
            if (refreshToken != null) {
                boolean renewalSuccess = attemptTokenRenewalWithRefreshOnly(refreshToken, response);

                if (renewalSuccess) {
                    log.debug("토큰 갱신 성공, 요청 계속 처리");
                } else {
                    log.debug("토큰 갱신 실패, 인증 없이 진행");
                }
            } else {
                log.debug("Access Token과 Refresh Token 모두 없음 - 인증 없이 진행");
            }
            filterChain.doFilter(request, response);
            return;
        }

        Claims claims = jwtProvider.parseClaim(requestAccessToken);

        try {
            if(userBlackListRepository.existsById(claims.getSubject())){
                log.warn("블랙리스트 사용자 접근 시도: {}", claims.getSubject());
                filterChain.doFilter(request, response);
                return;
            }
        } catch (Exception e) {
            log.warn("블랙리스트 확인 실패 (무시하고 진행): {}", e.getMessage());
        }

        try {
            if(jwtProvider.validateToken(requestAccessToken)){
                Authentication authentication = jwtProvider.generateAuthentication(requestAccessToken);
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("Access Token 인증 성공: {}", claims.getSubject());
            } else {
                log.debug("Access Token 검증 실패");
            }
        } catch (ExpiredJwtException ex) {
            log.debug("Access Token 만료, 갱신 시도");
            AccessTokenDto newAccessToken = renewingAccessToken(requestAccessToken, request);
            if (newAccessToken == null) {
                log.debug("토큰 갱신 실패");
                filterChain.doFilter(request, response);
                return;
            }

            RefreshToken newRefreshToken = renewingRefreshToken(claims.getId(), newAccessToken.getId());
            if (newRefreshToken != null) {
                responseToken(response, newAccessToken, newRefreshToken);

                Authentication authentication = jwtProvider.generateAuthentication(newAccessToken.getToken());
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("토큰 갱신 및 인증 성공: {}", claims.getSubject());
            } else {
                log.debug("RefreshToken 갱신 실패");
            }
        }

        Authentication finalAuth = SecurityContextHolder.getContext().getAuthentication();
        if (finalAuth != null) {
            log.debug("SecurityContext - 사용자: {}, 권한: {}, 인증됨: {}",
                    finalAuth.getName(), finalAuth.getAuthorities(), finalAuth.isAuthenticated());
        } else {
            log.debug("SecurityContext에 인증 정보 없음");
        }

        filterChain.doFilter(request, response);
    }

    private boolean attemptTokenRenewalWithRefreshOnly(String refreshToken, HttpServletResponse response) {
        try {
            RefreshToken storedRefreshToken = refreshTokenService.findByToken(refreshToken);

            if (storedRefreshToken == null) {
                log.warn("유효하지 않은 Refresh Token");
                return false;
            }

            String userId = storedRefreshToken.getUserId();
            if (userId == null) {
                log.warn("Refresh Token에서 사용자 정보를 찾을 수 없음");
                return false;
            }

            try {
                if(userBlackListRepository.existsById(userId)){
                    log.warn("블랙리스트 사용자의 토큰 갱신 시도: {}", userId);
                    return false;
                }
            } catch (Exception e) {
                log.warn("블랙리스트 확인 실패: {}", e.getMessage());
            }

            AccessTokenDto newAccessToken = jwtProvider.generateAccessToken(userId);

            RefreshToken newRefreshToken = refreshTokenService.renewingToken(
                    storedRefreshToken.getAccessTokenId(), newAccessToken.getId());

            if (newRefreshToken != null) {
                responseToken(response, newAccessToken, newRefreshToken);

                Authentication authentication = jwtProvider.generateAuthentication(newAccessToken.getToken());
                SecurityContextHolder.getContext().setAuthentication(authentication);

                return true;
            } else {
                log.error("RefreshToken 갱신 실패");
                return false;
            }

        } catch (Exception e) {
            log.error("Refresh Token으로 토큰 갱신 중 오류: {}", e.getMessage());
            return false;
        }
    }

    private void responseToken(HttpServletResponse response, AccessTokenDto newAccessToken,
                               RefreshToken newRefreshToken) {

        CookieUtils.addCookie(response, "ACCESS_TOKEN", newAccessToken.getToken(),
                Math.toIntExact(newAccessToken.getExpiresIn() / 1000), "/");
        CookieUtils.addCookie(response, "REFRESH_TOKEN", newRefreshToken.getToken(),
                Math.toIntExact(jwtProvider.getRtExpiration() / 1000), "/");
    }

    private RefreshToken renewingRefreshToken(String accessTokenId, String newTokenId) {
        try {
            return refreshTokenService.renewingToken(accessTokenId, newTokenId);
        } catch (Exception e) {
            log.error("RefreshToken 갱신 실패: {}", e.getMessage());
            return null;
        }
    }

    private AccessTokenDto renewingAccessToken(String requestAccessToken, HttpServletRequest request) {
        try {
            Authentication authentication = jwtProvider.generateAuthentication(requestAccessToken);
            String refreshToken = jwtProvider.resolveToken(request, TokenType.REFRESH_TOKEN);
            Claims claims = jwtProvider.parseClaim(requestAccessToken);

            RefreshToken storedRefreshToken = refreshTokenService.findByAccessTokenId(claims.getId());

            if(storedRefreshToken == null) {
                log.warn("저장된 Refresh Token이 없음");
                return null;
            }

            if (!storedRefreshToken.getToken().equals(refreshToken)) {
                log.error("Refresh Token 불일치 - 보안 위반 감지: {}", authentication.getName());
                try {
                    userBlackListRepository.save(new UserBlackList(authentication.getName()));
                } catch (Exception e) {
                    log.warn("블랙리스트 저장 실패: {}", e.getMessage());
                }
                throw new CommonException(ResponseCode.UNAUTHORIZED);
            }

            return generateAccessToken(authentication);
        } catch (CommonException e) {
            throw e;
        } catch (Exception e) {
            log.error("Access Token 갱신 중 오류: {}", e.getMessage());
            return null;
        }
    }

    private AccessTokenDto generateAccessToken(Authentication authentication) {
        AccessTokenDto newAccessToken = jwtProvider.generateAccessToken(authentication);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return newAccessToken;
    }
}
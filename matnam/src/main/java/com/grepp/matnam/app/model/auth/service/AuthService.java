package com.grepp.matnam.app.model.auth.service;

import com.grepp.matnam.app.model.auth.token.dto.AccessTokenDto;
import com.grepp.matnam.app.model.auth.token.dto.TokenDto;
import com.grepp.matnam.app.model.auth.token.entity.RefreshToken;
import com.grepp.matnam.app.model.auth.token.repository.UserBlackListRepository;
import com.grepp.matnam.app.model.auth.token.service.RefreshTokenService;
import com.grepp.matnam.app.model.user.entity.User;
import com.grepp.matnam.app.model.user.service.UserService;
import com.grepp.matnam.infra.auth.jwt.JwtProvider;
import com.grepp.matnam.infra.auth.jwt.code.TokenType;
import com.grepp.matnam.infra.error.exceptions.CommonException;
import com.grepp.matnam.infra.response.ResponseCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AuthService {

    private final RefreshTokenService refreshTokenService;
    private final UserBlackListRepository userBlackListRepository;
    private final JwtProvider jwtProvider;
    private final UserService userService;

    @Transactional
    public TokenDto signup(User user) {
        User savedUser = userService.signup(user);

        return processTokenSignin(savedUser.getUserId());
    }

    @Transactional
    public TokenDto signin(String userId, String password) {
        User user = userService.signin(userId, password);

        return processTokenSignin(userId);
    }

    @Transactional
    public TokenDto processTokenSignin(String userId) {
        try {
            try {
                userBlackListRepository.deleteById(userId);
            } catch (Exception e) {
                log.warn("블랙리스트 제거 실패: {}", e.getMessage());
            }

            try {
                refreshTokenService.deleteByUserId(userId);
            } catch (Exception e) {
                log.warn("기존 Refresh Token 삭제 실패: {}", e.getMessage());
            }

            AccessTokenDto accessTokenDto = jwtProvider.generateAccessToken(userId);

            RefreshToken refreshToken = null;
            try {
                refreshToken = new RefreshToken(userId, accessTokenDto.getId());
                refreshToken = refreshTokenService.save(refreshToken);
            } catch (Exception e) {
                log.error("RefreshToken Redis 저장 실패: {}", e.getMessage());

                refreshToken = new RefreshToken(userId, accessTokenDto.getId());
                refreshToken.setToken("temp_" + accessTokenDto.getId());
                log.warn("임시 RefreshToken 생성: {}", userId);
            }

            log.info("토큰 발급 완료: {}", userId);

            return TokenDto.builder()
                    .accessToken(accessTokenDto.getToken())
                    .refreshToken(refreshToken.getToken())
                    .atExpiresIn(jwtProvider.getAtExpiration())
                    .rtExpiresIn(jwtProvider.getRtExpiration())
                    .grantType("Bearer")
                    .build();

        } catch (Exception e) {
            log.error("토큰 발급 중 오류 발생: {}", e.getMessage(), e);
            throw new CommonException(ResponseCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    public TokenDto refreshToken(HttpServletRequest request) {
        log.info("토큰 갱신 요청");

        String refreshToken = jwtProvider.resolveToken(request, TokenType.REFRESH_TOKEN);
        if (refreshToken == null) {
            log.warn("Refresh Token이 없음");
            throw new CommonException(ResponseCode.UNAUTHORIZED);
        }

        try {
            RefreshToken storedRefreshToken = refreshTokenService.findByToken(refreshToken);
            if (storedRefreshToken == null) {
                if (refreshToken.startsWith("temp_")) {
                    log.warn("임시 토큰으로 갱신 시도 - Redis 연결 후 재로그인 필요");
                    throw new CommonException(ResponseCode.UNAUTHORIZED);
                }

                log.warn("유효하지 않은 Refresh Token");
                throw new CommonException(ResponseCode.UNAUTHORIZED);
            }

            AccessTokenDto newAccessToken = jwtProvider.generateAccessToken(storedRefreshToken.getUserId());

            RefreshToken newRefreshToken = refreshTokenService.renewingToken(
                    storedRefreshToken.getAccessTokenId(), newAccessToken.getId());

            log.info("토큰 갱신 완료: {}", storedRefreshToken.getUserId());

            return TokenDto.builder()
                    .accessToken(newAccessToken.getToken())
                    .refreshToken(newRefreshToken.getToken())
                    .atExpiresIn(jwtProvider.getAtExpiration())
                    .rtExpiresIn(jwtProvider.getRtExpiration())
                    .grantType("Bearer")
                    .build();

        } catch (CommonException e) {
            throw e;
        } catch (Exception e) {
            log.error("토큰 갱신 중 Redis 오류: {}", e.getMessage());
            throw new CommonException(ResponseCode.UNAUTHORIZED);
        }
    }

    @Transactional
    public void logout(String userId) {
        log.info("로그아웃 처리: {}", userId);

        try {
            refreshTokenService.deleteByUserId(userId);
        } catch (Exception e) {
            log.warn("Refresh Token 삭제 실패: {}", e.getMessage());
        }

        SecurityContextHolder.clearContext();
    }

    @Transactional
    public TokenDto processOAuth2Signin(String userId) {
        return processTokenSignin(userId);
    }
}
package com.grepp.matnam.app.model.auth.service;

import com.grepp.matnam.app.model.auth.token.dto.AccessTokenDto;
import com.grepp.matnam.app.model.auth.token.dto.TokenDto;
import com.grepp.matnam.app.model.auth.token.entity.RefreshToken;
import com.grepp.matnam.app.model.auth.token.repository.UserBlackListRepository;
import com.grepp.matnam.app.model.auth.token.service.RefreshTokenService;
import com.grepp.matnam.app.model.outbox.entity.OutBox;
import com.grepp.matnam.app.model.outbox.repository.OutBoxRepository;
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
    private final OutBoxRepository outBoxRepository;

    @Transactional
    public TokenDto signup(User user) {
        User savedUser = userService.signup(user);
        OutBox outbox = new OutBox("SIGNUP_COMPLETE", savedUser.getEmail(), savedUser.getNickname(),
            savedUser.getEmailCode());
        outBoxRepository.save(outbox);
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
            cleanupExistingTokens(userId);

            AccessTokenDto accessTokenDto = jwtProvider.generateAccessToken(userId);
            RefreshToken refreshToken = createAndSaveRefreshToken(userId, accessTokenDto.getId());

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

        String refreshTokenValue = jwtProvider.resolveToken(request, TokenType.REFRESH_TOKEN);
        if (refreshTokenValue == null) {
            log.warn("Refresh Token이 없음");
            throw new CommonException(ResponseCode.UNAUTHORIZED);
        }

        return refreshTokenWithValue(refreshTokenValue);
    }

    @Transactional
    public TokenDto refreshTokenWithValue(String refreshTokenValue) {
        try {
            RefreshToken storedRefreshToken = refreshTokenService.findByToken(refreshTokenValue);
            if (storedRefreshToken == null) {
                if (refreshTokenValue.startsWith("temp_")) {
                    throw new CommonException(ResponseCode.UNAUTHORIZED);
                }

                log.warn("유효하지 않은 Refresh Token");
                throw new CommonException(ResponseCode.UNAUTHORIZED);
            }

            String userId = storedRefreshToken.getUserId();

            try {
                if (userBlackListRepository.existsById(userId)) {
                    log.warn("블랙리스트 사용자의 토큰 갱신 시도: {}", userId);
                    throw new CommonException(ResponseCode.UNAUTHORIZED);
                }
            } catch (CommonException e) {
                throw e;
            } catch (Exception e) {
                log.warn("블랙리스트 확인 실패: {}", e.getMessage());
            }

            AccessTokenDto newAccessToken = jwtProvider.generateAccessToken(userId);

            RefreshToken newRefreshToken = refreshTokenService.renewingToken(
                    storedRefreshToken.getAccessTokenId(), newAccessToken.getId());

            if (newRefreshToken == null) {
                log.error("Refresh Token 갱신 실패: {}", userId);
                throw new CommonException(ResponseCode.INTERNAL_SERVER_ERROR);
            }

            log.info("토큰 갱신 완료: {}", userId);

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

    private void cleanupExistingTokens(String userId) {
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
    }

    private RefreshToken createAndSaveRefreshToken(String userId, String accessTokenId) {
        try {
            RefreshToken refreshToken = new RefreshToken(userId, accessTokenId);
            return refreshTokenService.save(refreshToken);
        } catch (Exception e) {
            log.error("RefreshToken Redis 저장 실패: {}", e.getMessage());

            RefreshToken tempRefreshToken = new RefreshToken(userId, accessTokenId);
            tempRefreshToken.setToken("temp_" + accessTokenId);
            log.warn("임시 RefreshToken 생성: {}", userId);

            return tempRefreshToken;
        }
    }
}
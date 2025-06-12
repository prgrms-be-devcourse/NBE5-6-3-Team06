package com.grepp.matnam.app.controller.api.auth;

import com.grepp.matnam.app.controller.api.auth.payload.SigninRequest;
import com.grepp.matnam.app.controller.api.auth.payload.SignupRequest;
import com.grepp.matnam.app.controller.api.auth.payload.TokenResponse;
import com.grepp.matnam.app.model.auth.service.AuthService;
import com.grepp.matnam.app.model.auth.token.dto.TokenDto;
import com.grepp.matnam.app.model.log.service.UserActivityLogService;
import com.grepp.matnam.app.model.user.entity.User;
import com.grepp.matnam.app.model.user.service.UserService;
import com.grepp.matnam.infra.auth.AuthenticationUtils;
import com.grepp.matnam.infra.auth.CookieUtils;
import com.grepp.matnam.infra.auth.jwt.JwtProvider;
import com.grepp.matnam.infra.response.ApiResponse;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication API", description = "인증 API (Access + Refresh Token)")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final UserActivityLogService userActivityLogService;
    private final UserService userService;
    private final JwtProvider jwtProvider;

    @PostMapping("/signup")
    @Operation(summary = "회원가입 + 자동 로그인", description = "회원가입과 동시에 토큰을 발급합니다.")
    public ResponseEntity<ApiResponse<TokenResponse>> signup(
            @Validated @RequestBody SignupRequest req,
            HttpServletResponse response
    ) {
        TokenDto dto = authService.signup(req.toEntity());

        User user = userService.getUserById(req.getUserId());

        setAuthCookies(response, dto, user);

        TokenResponse tokenResponse = TokenResponse.builder()
                .accessToken(dto.getAccessToken())
                .refreshToken(dto.getRefreshToken())
                .expiresIn(dto.getAtExpiresIn())
                .grantType(dto.getGrantType())
                .build();

        return ResponseEntity.ok(ApiResponse.success(tokenResponse));
    }

    @PostMapping("/signin")
    @Operation(summary = "로그인", description = "Access + Refresh Token을 발급합니다.")
    public ResponseEntity<ApiResponse<TokenResponse>> signin(
            @Validated @RequestBody SigninRequest req,
            HttpServletResponse response
    ) {
        TokenDto dto = authService.signin(req.getUserId(), req.getPassword());

        User user = userService.getUserById(req.getUserId());

        setAuthCookies(response, dto, user);

        userActivityLogService.logIfFirstToday(req.getUserId());

        TokenResponse tokenResponse = TokenResponse.builder()
                .accessToken(dto.getAccessToken())
                .refreshToken(dto.getRefreshToken())
                .expiresIn(dto.getAtExpiresIn())
                .grantType(dto.getGrantType())
                .build();

        log.info("로그인 성공 - 사용자: {}, 역할: {}", user.getUserId(), user.getRole());

        return ResponseEntity.ok(ApiResponse.success(tokenResponse));
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "Refresh Token을 삭제하고 쿠키를 초기화합니다.")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest request,
            HttpServletResponse response
    ){
        try {
            String userId = AuthenticationUtils.getCurrentUserId();
            if (userId != null) {
                authService.logout(userId);
            }
        } catch (Exception e) {
            log.warn("로그아웃 처리 중 오류: {}", e.getMessage());
        }

        CookieUtils.clearAllCookies(request, response);

        return ResponseEntity.ok(ApiResponse.noContent());
    }

    @PostMapping("/refresh")
    @Operation(summary = "토큰 갱신", description = "Refresh Token으로 새로운 Access Token을 발급합니다.")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        TokenDto dto = authService.refreshToken(request);

        try {
            String userId = AuthenticationUtils.getCurrentUserId();
            if (userId == null) {
                Claims claims = jwtProvider.parseClaim(dto.getAccessToken());
                userId = claims.getSubject();
            }

            User user = userService.getUserById(userId);

            setAuthCookies(response, dto, user);
        } catch (Exception e) {
            log.warn("토큰 갱신 시 사용자 정보 쿠키 설정 실패: {}", e.getMessage());
            CookieUtils.addCookie(response, "ACCESS_TOKEN", dto.getAccessToken(),
                    Math.toIntExact(dto.getAtExpiresIn() / 1000), "/");
            CookieUtils.addCookie(response, "REFRESH_TOKEN", dto.getRefreshToken(),
                    Math.toIntExact(dto.getRtExpiresIn() / 1000), "/");
        }

        TokenResponse tokenResponse = TokenResponse.builder()
                .accessToken(dto.getAccessToken())
                .refreshToken(dto.getRefreshToken())
                .expiresIn(dto.getAtExpiresIn())
                .grantType(dto.getGrantType())
                .build();

        return ResponseEntity.ok(ApiResponse.success(tokenResponse));
    }

    private void setAuthCookies(HttpServletResponse response, TokenDto dto, User user) {
        int accessTokenMaxAge = Math.toIntExact(dto.getAtExpiresIn() / 1000);
        int refreshTokenMaxAge = Math.toIntExact(dto.getRtExpiresIn() / 1000);

        CookieUtils.addCookie(response, "ACCESS_TOKEN", dto.getAccessToken(), accessTokenMaxAge, "/");
        CookieUtils.addCookie(response, "REFRESH_TOKEN", dto.getRefreshToken(), refreshTokenMaxAge, "/");
    }
}
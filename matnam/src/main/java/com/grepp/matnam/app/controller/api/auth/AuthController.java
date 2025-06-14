package com.grepp.matnam.app.controller.api.auth;

import com.grepp.matnam.app.controller.api.auth.payload.SigninRequest;
import com.grepp.matnam.app.controller.api.auth.payload.SignupRequest;
import com.grepp.matnam.app.controller.api.auth.payload.TokenResponse;
import com.grepp.matnam.app.model.auth.service.AuthService;
import com.grepp.matnam.app.model.auth.token.dto.TokenDto;
import com.grepp.matnam.app.model.kafka.service.KafkaProducerService;
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
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

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
    private final KafkaProducerService kafkaProducerService;

    @PostMapping("/signup")
    @Operation(summary = "회원가입 + 자동 로그인", description = "회원가입과 동시에 토큰을 발급합니다.")
    public ResponseEntity<ApiResponse<TokenResponse>> signup(
            @Validated @RequestBody SignupRequest request,
            HttpServletResponse response
    ) {
        try {
            TokenDto dto = authService.signup(request.toEntity());
            User user = userService.getUserById(request.getUserId());

            setAuthCookies(response, dto);

            kafkaProducerService.sendSignupEvent(request);

            TokenResponse tokenResponse = TokenResponse.builder()
                    .accessToken(dto.getAccessToken())
                    .refreshToken(dto.getRefreshToken())
                    .expiresIn(dto.getAtExpiresIn())
                    .grantType(dto.getGrantType())
                    .build();

            return ResponseEntity.ok(ApiResponse.success(tokenResponse));

        } catch (Exception e) {
            log.error("회원가입 처리 중 오류: {}", e.getMessage());
            throw e;
        }
    }

    @PostMapping("/signin")
    @Operation(summary = "로그인", description = "Access + Refresh Token을 발급합니다.")
    public ResponseEntity<ApiResponse<TokenResponse>> signin(
            @Validated @RequestBody SigninRequest request,
            HttpServletResponse response
    ) {
        try {
            TokenDto dto = authService.signin(request.getUserId(), request.getPassword());
            User user = userService.getUserById(request.getUserId());

            setAuthCookies(response, dto);

            userActivityLogService.logIfFirstToday(request.getUserId());

            TokenResponse tokenResponse = TokenResponse.builder()
                    .accessToken(dto.getAccessToken())
                    .refreshToken(dto.getRefreshToken())
                    .expiresIn(dto.getAtExpiresIn())
                    .grantType(dto.getGrantType())
                    .build();

            log.info("로그인 성공 - 사용자: {}, 역할: {}", user.getUserId(), user.getRole());
            return ResponseEntity.ok(ApiResponse.success(tokenResponse));

        } catch (Exception e) {
            log.error("로그인 처리 중 오류: {}", e.getMessage());
            throw e;
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "Refresh Token을 삭제하고 쿠키를 초기화합니다.")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        try {
            String userId = AuthenticationUtils.getCurrentUserId();
            if (userId != null) {
                authService.logout(userId);
            } else {
                log.warn("로그아웃 요청 오류: 현재 사용자 정보를 찾을 수 없음");
            }
        } catch (Exception e) {
            log.warn("로그아웃 처리 중 오류: {}", e.getMessage());
        } finally {
            CookieUtils.clearAllCookies(request, response);
        }

        return ResponseEntity.ok(ApiResponse.noContent());
    }

    @PostMapping("/refresh")
    @Operation(summary = "토큰 갱신", description = "Refresh Token으로 새로운 Access Token을 발급합니다.")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        try {
            TokenDto dto = authService.refreshToken(request);

            String userId = extractUserIdFromToken(dto.getAccessToken());
            if (userId != null) {
                setAuthCookies(response, dto);
                log.info("토큰 갱신 성공: {}", userId);
            } else {
                log.warn("토큰 갱신 성공했지만 사용자 정보 추출 실패");
                setAuthCookies(response, dto);
            }

            TokenResponse tokenResponse = TokenResponse.builder()
                    .accessToken(dto.getAccessToken())
                    .refreshToken(dto.getRefreshToken())
                    .expiresIn(dto.getAtExpiresIn())
                    .grantType(dto.getGrantType())
                    .build();

            return ResponseEntity.ok(ApiResponse.success(tokenResponse));

        } catch (Exception e) {
            log.error("토큰 갱신 처리 중 오류: {}", e.getMessage());
            throw e;
        }
    }

    private void setAuthCookies(HttpServletResponse response, TokenDto dto) {
        try {
            CookieUtils.addTokenCookie(response, "ACCESS_TOKEN", dto.getAccessToken(), "/");
            CookieUtils.addTokenCookie(response, "REFRESH_TOKEN", dto.getRefreshToken(), "/");

            log.debug("토큰 쿠키 설정 완료 - 쿠키 유효시간: 7일");
        } catch (Exception e) {
            log.error("토큰 쿠키 설정 실패: {}", e.getMessage());
            throw e;
        }
    }

    private String extractUserIdFromToken(String accessToken) {
        try {
            Claims claims = jwtProvider.parseClaim(accessToken);
            return claims.getSubject();
        } catch (Exception e) {
            log.warn("토큰에서 사용자 ID 추출 실패: {}", e.getMessage());
            return null;
        }
    }

    @GetMapping("/validate")
    @Operation(summary = "토큰 유효성 검증", description = "현재 사용자의 토큰이 유효한지 확인합니다.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateToken() {
        String currentUserId = AuthenticationUtils.getCurrentUserId();

        Map<String, Object> result = new HashMap<>();
        result.put("valid", true);
        result.put("userId", currentUserId);
        result.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
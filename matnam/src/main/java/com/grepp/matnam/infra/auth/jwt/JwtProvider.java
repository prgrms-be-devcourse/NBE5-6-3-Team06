package com.grepp.matnam.infra.auth.jwt;

import com.grepp.matnam.app.model.auth.token.dto.AccessTokenDto;
import com.grepp.matnam.app.model.user.entity.User;
import com.grepp.matnam.app.model.user.service.UserService;
import com.grepp.matnam.infra.auth.jwt.code.TokenType;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class JwtProvider {
    private final UserService userService;

    @Value("${jwt.secret}")
    private String key;

    @Getter
    @Value("${jwt.access-expiration:300000}")
    private long atExpiration;

    @Getter
    @Value("${jwt.refresh-expiration:604800000}")
    private long rtExpiration;

    private SecretKey secretKey;

    public SecretKey getSecretKey() {
        if (secretKey == null) {
            String base64Key = Base64.getEncoder().encodeToString(key.getBytes());
            secretKey = Keys.hmacShaKeyFor(base64Key.getBytes(StandardCharsets.UTF_8));
        }
        return secretKey;
    }

    public AccessTokenDto generateAccessToken(String userId) {
        String id = UUID.randomUUID().toString();
        long now = new Date().getTime();
        Date atExpiresIn = new Date(now + atExpiration);

        User user = userService.getUserById(userId);

        String roleString = user.getRole().name();

        String accessToken = Jwts.builder()
                .subject(userId)
                .id(id)
                .claim("role", roleString)
                .claim("nickname", user.getNickname())
                .expiration(atExpiresIn)
                .signWith(getSecretKey())
                .compact();

        return AccessTokenDto.builder()
                .id(id)
                .token(accessToken)
                .expiresIn(atExpiration)
                .build();
    }

    public AccessTokenDto generateAccessToken(Authentication authentication) {
        return generateAccessToken(authentication.getName());
    }

    public Authentication generateAuthentication(String accessToken) {
        Claims claims = parseClaim(accessToken);
        String userId = claims.getSubject();
        String role = claims.get("role", String.class);

        if (role == null || role.isEmpty()) {
            role = "ROLE_ANONYMOUS";
        }

        if (!role.startsWith("ROLE_")) {
            role = "ROLE_" + role;
        }

        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role));

        return new UsernamePasswordAuthenticationToken(userId, "", authorities);
    }

    public Claims parseClaim(String accessToken) {
        if (!isValidTokenFormat(accessToken)) {
            log.warn("잘못된 토큰 형식: {}", accessToken != null ? accessToken.length() + "글자" : "null");
            return null;
        }

        try {
            try {
                return Jwts.parser().verifyWith(getSecretKey()).build()
                        .parseSignedClaims(accessToken).getPayload();
            } catch (ExpiredJwtException ex) {
                return ex.getClaims();
            }
        } catch (Exception e) {
            log.warn("토큰 파싱 실패: {}", e.getMessage());
            return null;
        }
    }

    public boolean validateToken(String requestAccessToken) {
        if (!isValidTokenFormat(requestAccessToken)) {
            log.debug("토큰 형식 검증 실패");
            return false;
        }

        try {
            Jwts.parser().verifyWith(getSecretKey()).build().parse(requestAccessToken);
            return true;
        } catch (SecurityException | MalformedJwtException | UnsupportedJwtException | IllegalArgumentException e) {
            log.error("JWT 토큰 검증 실패: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.debug("JWT 토큰 만료: {}", e.getMessage());
        }
        return false;
    }

    private boolean isValidTokenFormat(String token) {
        if (!StringUtils.hasText(token)) {
            return false;
        }

        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return false;
        }

        for (String part : parts) {
            if (!StringUtils.hasText(part)) {
                return false;
            }
        }

        return true;
    }

    public String resolveToken(HttpServletRequest request, TokenType tokenType) {
        if (tokenType == TokenType.ACCESS_TOKEN) {
            String headerToken = request.getHeader("Authorization");
            if (StringUtils.hasText(headerToken) && headerToken.startsWith("Bearer ")) {
                String token = headerToken.substring(7);
                return isValidTokenFormat(token) ? token : null;
            }
        }

        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            log.debug("쿠키가 없음");
            return null;
        }

        String cookieName = tokenType == TokenType.ACCESS_TOKEN ? "ACCESS_TOKEN" : "REFRESH_TOKEN";

        String token = Arrays.stream(cookies)
                .filter(cookie -> cookieName.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(null);

        if (token != null) {
            if (tokenType == TokenType.ACCESS_TOKEN) {
                if (!isValidTokenFormat(token)) {
                    log.warn("잘못된 Access Token 형식: {}", token.length() + "글자");
                    return null;
                }
            }
            return token;
        } else {
            return null;
        }
    }
}
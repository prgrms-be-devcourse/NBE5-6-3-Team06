package com.grepp.matnam.infra.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

@Slf4j
@Component
public class CookieUtils {

    public static void addCookie(HttpServletResponse response, String name, String value, int maxAge, String path) {
        Cookie cookie = new Cookie(name, value);
        cookie.setMaxAge(maxAge);
        cookie.setPath(path);
        cookie.setHttpOnly(false);
        cookie.setSecure(false);

        response.addCookie(cookie);
        log.debug("쿠키 생성: {}, 유효시간: {}초", name, maxAge);
    }

    public static void addTokenCookie(HttpServletResponse response, String name, String value, String path) {
        Cookie cookie = new Cookie(name, value);
        cookie.setMaxAge(7 * 24 * 60 * 60); // 7일
        cookie.setPath(path);
        cookie.setHttpOnly(false);
        cookie.setSecure(false);

        response.addCookie(cookie);
        log.debug("토큰 쿠키 생성: {}, 쿠키 유효시간: 7일", name);
    }

    public static void addUserNicknameCookie(HttpServletResponse response, String nickname, int maxAge) {
        String encodedNickname = URLEncoder.encode(nickname, StandardCharsets.UTF_8);
        addCookie(response, "userNickname", encodedNickname, maxAge, "/");
    }

    public static Optional<Cookie> getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            return Arrays.stream(cookies)
                    .filter(cookie -> name.equals(cookie.getName()))
                    .findFirst();
        }
        return Optional.empty();
    }

    public static Optional<String> getAccessToken(HttpServletRequest request) {
        return getCookie(request, "ACCESS_TOKEN")
                .map(Cookie::getValue);
    }

    public static void clearAllCookies(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (isAuthRelatedCookie(cookie.getName())) {
                    Cookie clearCookie = new Cookie(cookie.getName(), "");
                    clearCookie.setPath("/");
                    clearCookie.setMaxAge(0);
                    clearCookie.setHttpOnly(false);
                    response.addCookie(clearCookie);
                    log.debug("쿠키 삭제: {}", cookie.getName());
                }
            }
        }
        log.debug("인증 관련 쿠키 삭제 완료");
    }

    private static boolean isAuthRelatedCookie(String cookieName) {
        return cookieName.equals("ACCESS_TOKEN") ||
                cookieName.equals("REFRESH_TOKEN") ||
                cookieName.equals("jwtToken") ||
                cookieName.equals("userId") ||
                cookieName.equals("userRole") ||
                cookieName.equals("userNickname");
    }
}
package com.grepp.matnam.infra.auth.oauth2;

import com.grepp.matnam.app.model.auth.service.AuthService;
import com.grepp.matnam.app.model.auth.token.dto.TokenDto;
import com.grepp.matnam.app.model.user.code.Role;
import com.grepp.matnam.app.model.user.repository.UserRepository;
import com.grepp.matnam.app.model.user.code.Gender;
import com.grepp.matnam.app.model.user.code.Status;
import com.grepp.matnam.app.model.user.dto.CustomOAuth2User;
import com.grepp.matnam.app.model.user.entity.User;
import com.grepp.matnam.infra.auth.CookieUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();

        String userId = oAuth2User.getUserId();
        String email = oAuth2User.getEmail();
        String name = oAuth2User.getName();

        log.info("OAuth2 로그인 성공: userId={}, email={}, name={}", userId, email, name);

        boolean isExistingUser = userRepository.existsByUserId(userId);

        if (!isExistingUser) {
            User newUser = new User();
            newUser.setUserId(userId);
            newUser.setEmail(email);
            newUser.setNickname(name);
            newUser.setPassword(passwordEncoder.encode("1234"));
            newUser.setRole(Role.ROLE_USER);
            newUser.setStatus(Status.ACTIVE);
            newUser.setTemperature(36.5f);
            newUser.setGender(Gender.MAN);
            newUser.setAge(30);

            User savedUser = userRepository.save(newUser);
            log.info("OAuth2 사용자 기본 정보 저장: {}", userId);

            TokenDto tokenDto = authService.processOAuth2Signin(userId);

            setAllAuthCookies(response, tokenDto, savedUser);

            response.sendRedirect("/user/oauth2/signup");

        } else {
            User user = userRepository.findByUserId(userId).orElse(null);
            if (user != null) {
                TokenDto tokenDto = authService.processOAuth2Signin(userId);

                setAllAuthCookies(response, tokenDto, user);

                if (user.getPreference() == null) {
                    response.sendRedirect("/user/preference");
                } else {
                    response.sendRedirect("/");
                }
            } else {
                response.sendRedirect("/user/signin?error=oauth_user_not_found");
            }
        }
    }

    private void setAllAuthCookies(HttpServletResponse response, TokenDto tokenDto, User user) {
        int accessTokenMaxAge = Math.toIntExact(tokenDto.getAtExpiresIn() / 1000);
        int refreshTokenMaxAge = Math.toIntExact(tokenDto.getRtExpiresIn() / 1000);

        CookieUtils.addCookie(response, "ACCESS_TOKEN", tokenDto.getAccessToken(), accessTokenMaxAge, "/");
        CookieUtils.addCookie(response, "REFRESH_TOKEN", tokenDto.getRefreshToken(), refreshTokenMaxAge, "/");
    }
}
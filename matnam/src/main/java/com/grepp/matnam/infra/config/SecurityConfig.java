package com.grepp.matnam.infra.config;

import com.grepp.matnam.app.model.user.service.CustomOauth2UserService;
import com.grepp.matnam.infra.auth.jwt.JwtAuthenticationFilter;
import com.grepp.matnam.infra.auth.jwt.LogoutFilter;
import com.grepp.matnam.infra.auth.oauth2.OAuth2AuthenticationSuccessHandler;
import com.grepp.matnam.infra.auth.security.CustomAccessDeniedHandler;
import com.grepp.matnam.infra.auth.security.CustomAuthenticationEntryPoint;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final LogoutFilter logoutFilter;
    private final CustomOauth2UserService customOauth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .headers(headers -> headers
                .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
            )
            .authorizeHttpRequests(auth -> auth
                // 1. 인증이 필요하지 않은 경로
                .requestMatchers(
                    "/", "/error", "/favicon.ico",
                    "/css/**", "/js/**", "/img/**", "/images/**", "/fonts/**", "/assets/**", "/download/**"
                ).permitAll()

                // 2. 인증 관련 API
                .requestMatchers(
                    "/api/auth/signin",
                    "/api/auth/signup",
                    "/api/auth/refresh"
                ).permitAll()

                // 회원가입/로그인 관련
                .requestMatchers(
                    "/user/signup", "/user/signin", "/user/oauth2/signup", "/user/verify"
                ).permitAll()

                // OAuth2 관련
                .requestMatchers("/signin/oauth2/code/**").permitAll()

                // Swagger/API 문서 관련
                .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**")
                .permitAll()

                // AI 테스트 관련
                .requestMatchers("/api/ai/chat", "/api/ai/restaurant/name").permitAll()

                // 팀 관련 공개 페이지
                .requestMatchers("/team/search", "/team/detail/**").permitAll()

                // 3. 관리자 권한이 필요한 경로
                .requestMatchers("/admin/**", "/api/admin/**").hasRole("ADMIN")

                // 4. 사용자 권한이 필요한 경로
                .requestMatchers("/user/**").hasRole("USER")

                // 로그아웃
                .requestMatchers("/api/auth/logout").hasAnyRole("USER", "ADMIN")

                // 사용자 전용 API (로그인 제외)
                .requestMatchers(
                    "/api/user/preference/**", "/api/user/password",
                    "/api/user/{userId}/temperature",   // 매너온도 조회
                    "/map/**", "/api/mymap/**",       // 지도 관련
                    "/api/reviews/**",                  // 리뷰 관련
                    "/api/reports/**"                   // 신고 관련
                ).hasRole("USER")

                // AI 추천 관련 (사용자)
                .requestMatchers("/api/ai/recommend/**", "/api/ai/reRecommend/**")
                .hasRole("USER")

                // 사용자 + 관리자 공통 API
                .requestMatchers(
                    "/api/chat/**",              // 채팅
                    "/api/sse/subscribe",        // SSE 구독
                    "/api/notification/**",      // 알림
                    "/api/content-rankings/**"   // 랭킹
                ).hasAnyRole("USER", "ADMIN")

                // 팀 관련 (인증된 사용자)
                .requestMatchers("/team/**", "/api/team/**").authenticated()

                // 5. 그 외 모든 요청은 인증 필요
                .anyRequest().authenticated()
            )
            .exceptionHandling(exceptionHandling -> exceptionHandling
                .authenticationEntryPoint(customAuthenticationEntryPoint)
                .accessDeniedHandler(customAccessDeniedHandler)
            )
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(endpoint -> endpoint
                    .userService(customOauth2UserService)
                )
                .redirectionEndpoint(endpoint -> endpoint
                    .baseUri("/signin/oauth2/code/*")
                )
                .successHandler(oAuth2AuthenticationSuccessHandler)
            )
            .addFilterBefore(logoutFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthenticationFilter, LogoutFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

//        TODO: 메일서버 추가시 주석 해제
//        configuration.setAllowedOrigins(List.of(
//                "http://localhost:8080" 메일 서버 추가
//        ));

        configuration.setAllowedMethods(List.of(
            "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));

        configuration.setAllowedHeaders(List.of(
            "Authorization", "Content-Type", "X-Requested-With", "Accept",
            "Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers"
        ));

        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        configuration.setExposedHeaders(List.of("Set-Cookie"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
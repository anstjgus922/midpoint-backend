package com.solucitation.midpoint_backend.global.config;

import com.solucitation.midpoint_backend.global.auth.JwtFilter;
import com.solucitation.midpoint_backend.global.auth.JwtTokenProvider;
import com.solucitation.midpoint_backend.global.exception.JwtAccessDeniedHandler;
import com.solucitation.midpoint_backend.global.exception.JwtAuthenticationEntryPoint;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.core.env.Environment;

/**
 * Spring Security 설정 클래스 - JWT를 사용한 보안 설정 구성
 */
@Slf4j
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    /**
     * SecurityConfig 생성자 - 필수 구성 요소 주입
     *
     * @param jwtTokenProvider JWT 토큰 제공자
     * @param jwtAuthenticationEntryPoint JWT 인증 진입점
     * @param jwtAccessDeniedHandler JWT 접근 거부 처리기
     */
    public SecurityConfig(
            JwtTokenProvider jwtTokenProvider,
            JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
            JwtAccessDeniedHandler jwtAccessDeniedHandler) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.jwtAccessDeniedHandler = jwtAccessDeniedHandler;
    }

    /**
     * 인증 관리자 빈 등록
     *
     * @param authenticationConfiguration 인증 설정
     * @return AuthenticationManager 인스턴스
     * @throws Exception 예외
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration
    ) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * 보안 필터 체인 설정
     *
     * @param http HttpSecurity 인스턴스
     * @return SecurityFilterChain 인스턴스
     * @throws Exception 예외
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // CSRF 비활성화
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // Stateless 세션 설정
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/api/auth/**", "/api/posts/**", "/midpoint/api/logic", "/api/s3/**").permitAll() // 인증 없이 접근 허용
                        .anyRequest().authenticated() // 나머지 요청은 인증 필요
                )
                .exceptionHandling(exception -> exception
                        .accessDeniedHandler(jwtAccessDeniedHandler) // 접근 거부 처리기 설정
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint) // 인증 진입점 설정
                )
                // Spring Security에서 OAuth2 로그인을 설정
                // OAuth2 인증이 성공적으로 완료된 후 리다이렉트할 URL을 설정
                .oauth2Login(oauth2 -> oauth2
                        .defaultSuccessUrl("/api/auth/oauth2/code/kakao")
                );

        // JWT 필터 추가
        http.addFilterBefore(new JwtFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }


    /**
     * CORS 설정 빈 등록
     *
     * @return CorsConfigurationSource 인스턴스
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.addAllowedOrigin("*"); // 모든 도메인 허용
        configuration.addAllowedMethod("*");
        configuration.addAllowedHeader("*");
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * 비밀번호 인코더 빈 등록
     *
     * @return BCryptPasswordEncoder 인스턴스
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
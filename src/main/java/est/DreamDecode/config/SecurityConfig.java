// src/main/java/est/DreamDecode/config/SecurityConfig.java
package est.DreamDecode.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.*;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwt;

    // ✅ 배포 시 이 배열만 바꾸면 됨
    private static final List<String> ALLOWED_ORIGINS = List.of(
            "http://localhost:3000"        // prod 예: "https://dreamdecode.app"
    );

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(reg -> reg
                        // ===== 정적 리소스 =====
                        .requestMatchers(
                                "/css/**", "/js/**", "/images/**",
                                "/webjars/**", "/favicon.ico"
                        ).permitAll()

                        // ===== 공개 페이지 =====
                        .requestMatchers(
                                "/",           // 메인 페이지
                                "/index",      // 메인 페이지
                                "/dream",      // 꿈 목록 페이지
                                "/error"       // 에러 페이지
                        ).permitAll()

                        // ===== 인증 관련 공개 API =====
                        .requestMatchers(
                                "/api/auth/**",        // 로그인, 리프레시 토큰
                                "/api/users/signup",   // 회원가입
                                "/api/email/**",       // 이메일 인증
                                "/api/password/**",    // 비밀번호 재설정
                                "/actuator/health"     // 헬스체크
                        ).permitAll()

                        // ===== 공개 꿈 조회 API (GET only) =====
                        .requestMatchers(HttpMethod.GET,
                                "/api/dream",                    // 전체 목록
                                "/api/dream/category/**",        // 카테고리 검색
                                "/api/dream/tag/**",             // 태그 검색
                                "/api/dream/title",              // 제목 검색
                                "/api/dream/*/analysis"          // 꿈 상세 (공개된 것만 - 서비스에서 체크)
                        ).permitAll()

                        // ===== 인증 필요한 페이지 =====
                        .requestMatchers("/profile").authenticated()

                        // ===== 인증 필요한 API =====
                        .requestMatchers(
                                "/api/users/me/**",      // 내 정보 조회/수정
                                "/api/dream/**"          // 꿈 등록/수정/삭제 (GET 제외, 위에서 허용)
                        ).authenticated()

                        // ===== 8. 그 외 모든 요청은 인증 필요 =====
                        .anyRequest().authenticated()
                )
                .addFilterBefore(new JwtTokenFilter(jwt), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(ALLOWED_ORIGINS);
        cfg.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        // 프론트에서 흔히 쓰는 헤더 + Authorization 허용
        cfg.setAllowedHeaders(List.of("Authorization","Content-Type","Accept","X-Requested-With"));
        // HttpOnly RT 쿠키 전송을 위해 필요
        cfg.setAllowCredentials(true);
        // 프리플라이트 캐시 1시간
        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
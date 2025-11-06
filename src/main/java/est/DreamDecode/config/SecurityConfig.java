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
                // SPA + JWT 구조: CSRF 불필요
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // 세션 없이 토큰 인증
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(reg -> reg
                        // ✅ 인증 없이 접근 가능한 공개 API
                        .requestMatchers("/api/auth/**", "/api/users/signup", "/api/email/**", "/actuator/health").permitAll()
                        // 정적/루트
                        .requestMatchers(HttpMethod.GET, "/", "/index.html", "/favicon.ico", "/assets/**").permitAll()
                        // Preflight(OPTIONS) 전역 허용
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // 그 외엔 인증 필요
                        .anyRequest().authenticated()
                )
                // 커스텀 401/403 핸들러는 나중에 추가
                .addFilterBefore(new JwtTokenFilter(jwt), UsernamePasswordAuthenticationFilter.class);

        // (선택) H2 콘솔 쓸 때만 열기
        // http.headers(h -> h.frameOptions(f -> f.sameOrigin()));

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

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

    // src/main/java/est/DreamDecode/config/SecurityConfig.java
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(reg -> reg
                        // ✅ 정적 리소스 & 루트 페이지는 모두 허용
                        .requestMatchers(
                                "/", "/index", "/error", "/favicon.ico",
                                "/css/**", "/js/**", "/images/**", "/profile",
                                "/webjars/**", "/assets/**"
                        ).permitAll()

                        // ✅ 공개 API
                        .requestMatchers(
                                "/api/auth/**",
                                "/api/users/signup",
                                "/api/email/**",
                                "/api/password/**",
                                "/actuator/health"
                        ).permitAll()

                        // ✅ 나머지는 인증 필요 (마이페이지, 내 정보 수정 등)
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

// src/main/java/est/DreamDecode/controller/AuthController.java
package est.DreamDecode.controller;

import est.DreamDecode.config.JwtTokenProvider;
import est.DreamDecode.dto.AuthResponse;
import est.DreamDecode.dto.ErrorResponse;
import est.DreamDecode.dto.LoginRequest;
import est.DreamDecode.exception.EmailNotVerifiedException;
import est.DreamDecode.service.AuthService;
import est.DreamDecode.util.CookieUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@Tag(name = "인증 API", description = "로그인, 로그아웃, 토큰 갱신 관련 API")
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwt; // 로그아웃 시 userId 추출용

    private static final String RT_COOKIE = "DD_RT";
    private static final String AT_COOKIE = "DD_AT";
    private static final int RT_MAX_AGE = 14 * 24 * 60 * 60; // 14일
    private static final int AT_MAX_AGE = 2 * 60 * 60; // 2시간

    /** 로그인: AT/RT → body + 쿠키 (페이지 접근을 위해 쿠키에도 저장) */
    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인합니다. Access Token과 Refresh Token이 발급되며, HttpOnly 쿠키로 저장됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
            @ApiResponse(responseCode = "401", description = "이메일 미인증 또는 비밀번호 불일치")
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(
                                      @RequestBody @Valid LoginRequest req,
                                      HttpServletResponse res
    ) {
        try {
            var tokens = authService.login(req.email(), req.password());
            // RT와 AT 모두 HttpOnly 쿠키로 저장 (XSS 공격 방지)
            CookieUtil.addHttpOnlyCookie(res, RT_COOKIE, tokens.refreshToken(), RT_MAX_AGE);
            CookieUtil.addHttpOnlyCookie(res, AT_COOKIE, tokens.accessToken(), AT_MAX_AGE);
            // Authorization 헤더 방식도 지원하므로 응답 body에 토큰 포함 (선택적)
            return ResponseEntity.ok(new AuthResponse(tokens.accessToken()));
        } catch (EmailNotVerifiedException e) {
            return ResponseEntity.status(401)
                    .body(new ErrorResponse(
                            "EMAIL_NOT_VERIFIED",
                            e.getMessage()
                    ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401)
                    .body(new ErrorResponse(
                            "INVALID_CREDENTIALS",
                            e.getMessage()
                    ));
        }
    }

    /** RT 재발급 */
    @Operation(summary = "토큰 갱신", description = "Refresh Token을 사용하여 Access Token과 Refresh Token을 재발급합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "토큰 갱신 성공"),
            @ApiResponse(responseCode = "401", description = "Refresh Token이 없거나 유효하지 않음")
    })
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(HttpServletRequest req, HttpServletResponse res) {
        String rt = extractCookie(req, RT_COOKIE);
        if (rt == null) return ResponseEntity.status(401).build();

        var tokens = authService.refresh(rt);
        // RT와 AT 모두 HttpOnly 쿠키로 갱신
        CookieUtil.addHttpOnlyCookie(res, RT_COOKIE, tokens.refreshToken(), RT_MAX_AGE);
        CookieUtil.addHttpOnlyCookie(res, AT_COOKIE, tokens.accessToken(), AT_MAX_AGE);
        return ResponseEntity.ok(new AuthResponse(tokens.accessToken()));
    }

    /** 로그아웃: 쿠키 제거 + 서버 RT 삭제 */
    @Operation(summary = "로그아웃", description = "서버의 Refresh Token을 삭제하고 클라이언트의 쿠키를 제거합니다.")
    @ApiResponse(responseCode = "204", description = "로그아웃 성공")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest req, HttpServletResponse res) {
        String rt = extractCookie(req, RT_COOKIE);
        if (rt != null) {
            try {
                Long userId = jwt.getUserId(rt);
                authService.logout(userId);
            } catch (Exception ignored) {}
        }
        // RT와 AT 모두 HttpOnly 쿠키로 삭제
        CookieUtil.deleteCookie(res, RT_COOKIE);
        CookieUtil.deleteCookie(res, AT_COOKIE);
        return ResponseEntity.noContent().build();
    }

    private String extractCookie(HttpServletRequest req, String name) {
        Cookie[] cookies = req.getCookies();
        if (cookies == null) return null;
        for (Cookie c : cookies) {
            if (name.equals(c.getName())) return c.getValue();
        }
        return null;
    }
}

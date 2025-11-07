// src/main/java/est/DreamDecode/controller/AuthController.java
package est.DreamDecode.controller;

import est.DreamDecode.config.JwtTokenProvider;
import est.DreamDecode.dto.AuthResponse;
import est.DreamDecode.dto.ErrorResponse;
import est.DreamDecode.dto.LoginRequest;
import est.DreamDecode.exception.EmailNotVerifiedException;
import est.DreamDecode.service.AuthService;
import est.DreamDecode.util.CookieUtil;
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
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwt; // 로그아웃 시 userId 추출용

    private static final String RT_COOKIE = "DD_RT";
    private static final String AT_COOKIE = "DD_AT";
    private static final int RT_MAX_AGE = 14 * 24 * 60 * 60; // 14일
    private static final int AT_MAX_AGE = 2 * 60 * 60; // 2시간

    /** 로그인: AT/RT → body + 쿠키 (페이지 접근을 위해 쿠키에도 저장) */
    @PostMapping("/login")
    public ResponseEntity<?> login(
                                      @RequestBody @Valid LoginRequest req,
                                      HttpServletResponse res
    ) {
        try {
            var tokens = authService.login(req.email(), req.password());
            // RT는 HttpOnly 쿠키 (보안 - JavaScript 접근 불가)
            CookieUtil.addHttpOnlyCookie(res, RT_COOKIE, tokens.refreshToken(), RT_MAX_AGE);
            // AT는 일반 쿠키 (JavaScript에서 읽기 가능 - 버튼 표시 등에 필요)
            CookieUtil.addCookie(res, AT_COOKIE, tokens.accessToken(), AT_MAX_AGE);
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
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(HttpServletRequest req, HttpServletResponse res) {
        String rt = extractCookie(req, RT_COOKIE);
        if (rt == null) return ResponseEntity.status(401).build();

        var tokens = authService.refresh(rt);
        // RT와 AT 모두 쿠키 갱신
        CookieUtil.addHttpOnlyCookie(res, RT_COOKIE, tokens.refreshToken(), RT_MAX_AGE);
        CookieUtil.addCookie(res, AT_COOKIE, tokens.accessToken(), AT_MAX_AGE);
        return ResponseEntity.ok(new AuthResponse(tokens.accessToken()));
    }

    /** 로그아웃: 쿠키 제거 + 서버 RT 삭제 */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest req, HttpServletResponse res) {
        String rt = extractCookie(req, RT_COOKIE);
        if (rt != null) {
            try {
                Long userId = jwt.getUserId(rt);
                authService.logout(userId);
            } catch (Exception ignored) {}
        }
        CookieUtil.deleteCookie(res, RT_COOKIE);
        CookieUtil.deleteNormalCookie(res, AT_COOKIE);
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

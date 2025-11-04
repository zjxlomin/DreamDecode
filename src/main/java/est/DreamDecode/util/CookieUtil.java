// src/main/java/est/DreamDecode/util/CookieUtil.java
package est.DreamDecode.util;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

import java.time.Duration;

/**
 * JWT RefreshToken 쿠키 관리 유틸
 * - HttpOnly / Secure / SameSite 속성 제어
 * - ResponseCookie 기반: CORS, 보안 대응
 */
public class CookieUtil {

    // ✅ 개발 중: secure=false, sameSite="Lax"
    // ✅ 배포 시: secure=true, sameSite="None"
    private static final boolean SECURE = false;     // HTTPS 환경에서만 true
    private static final String SAME_SITE = "Lax";   // prod: "None"

    public static void addHttpOnlyCookie(
            HttpServletResponse res,
            String name,
            String value,
            int maxAgeSec
    ) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(SECURE)
                .path("/")
                .maxAge(Duration.ofSeconds(maxAgeSec))
                .sameSite(SAME_SITE)
                .build();

        res.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public static void deleteCookie(HttpServletResponse res, String name) {
        ResponseCookie cookie = ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(SECURE)
                .path("/")
                .maxAge(Duration.ZERO)
                .sameSite(SAME_SITE)
                .build();

        res.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}

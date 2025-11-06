package est.DreamDecode.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties props;

    private Key key() {
        // HS256은 최소 256bit(32바이트) 이상 비밀키 권장
        return Keys.hmacShaKeyFor(props.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(Long userId, String email) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + props.getAccessTokenValidityMs());

        log.info("AT 발급: now={}, exp={}, ttlMs={}",
                now, exp, props.getAccessTokenValidityMs());

        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setIssuedAt(now)
                .setExpiration(exp)
                .claim("email", email)
                .claim("typ", "AT")
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }


    public String createRefreshToken(Long userId) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + props.getRefreshTokenValidityMs());
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setIssuedAt(now)
                .setExpiration(exp)
                .claim("typ", "RT")
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Jws<Claims> parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key())
                .setAllowedClockSkewSeconds(5)
                .build()
                .parseClaimsJws(token);
    }

    public Long getUserId(String token) {
        return Long.valueOf(parseToken(token).getBody().getSubject());
    }

    public boolean isAccessToken(String token) {
        Object typ = parseToken(token).getBody().get("typ");
        return "AT".equals(typ);
    }

    public boolean isRefreshToken(String token) {
        Object typ = parseToken(token).getBody().get("typ");
        return "RT".equals(typ);
    }

    /** 만료/서명 오류 등을 boolean으로 단순 판별 */
    public boolean validate(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}

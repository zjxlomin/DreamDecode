package est.DreamDecode.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider jwt;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        // 32바이트 이상 랜덤 문자열 (그냥 테스트니까 적당히 길게)
        props.setSecret("this_is_a_very_long_test_jwt_secret_key_1234567890");
        props.setAccessTokenValidityMs(1000L * 60 * 15);        // 15분
        props.setRefreshTokenValidityMs(1000L * 60 * 60 * 24);  // 1일

        jwt = new JwtTokenProvider(props);
    }

    @Test
    void accessToken_생성_및_파싱이_된다() {
        // given
        Long userId = 1L;
        String email = "test@example.com";

        // when
        String at = jwt.createAccessToken(userId, email);
        Jws<Claims> parsed = jwt.parseToken(at);
        Claims body = parsed.getBody();

        // then
        assertThat(body.getSubject()).isEqualTo(String.valueOf(userId));
        assertThat(body.get("email", String.class)).isEqualTo(email);
        assertThat(body.get("typ", String.class)).isEqualTo("AT");
        assertThat(jwt.isAccessToken(at)).isTrue();
        assertThat(jwt.isRefreshToken(at)).isFalse();
        assertThat(jwt.validate(at)).isTrue();
    }

    @Test
    void refreshToken_생성_및_타입_확인() {
        // given
        Long userId = 5L;

        // when
        String rt = jwt.createRefreshToken(userId);
        Jws<Claims> parsed = jwt.parseToken(rt);
        Claims body = parsed.getBody();

        // then
        assertThat(body.getSubject()).isEqualTo(String.valueOf(userId));
        assertThat(body.get("typ", String.class)).isEqualTo("RT");
        assertThat(jwt.isRefreshToken(rt)).isTrue();
        assertThat(jwt.isAccessToken(rt)).isFalse();
        assertThat(jwt.validate(rt)).isTrue();
    }
}

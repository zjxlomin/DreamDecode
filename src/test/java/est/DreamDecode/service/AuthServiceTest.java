package est.DreamDecode.service;

import est.DreamDecode.config.JwtTokenProvider;
import est.DreamDecode.domain.RefreshToken;
import est.DreamDecode.domain.User;
import est.DreamDecode.repository.RefreshTokenRepository;
import est.DreamDecode.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    RefreshTokenRepository refreshTokenRepository;

    @Mock
    JwtTokenProvider jwtTokenProvider;

    @Mock
    BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    AuthService authService;

    private User dummyUser() {
        return User.builder()
                .email("p500wh@naver.com")
                .password("ENC_PW")
                .name("박정훈")
                .birthday(LocalDate.of(1994, 1, 23))
                .gender(1)  // 남성이라면 1, 여성이라면 2 등으로
                .build();
    }


    @Nested
    @DisplayName("login() 테스트")
    class LoginTests {

        @Test
        @DisplayName("정상 로그인 시 AT/RT 발급 + RT 저장")
        void login_success() {
            // given
            String email = "p500wh@naver.com";
            String rawPw = "12345678";

            User user = dummyUser();

            when(userRepository.findByEmail(email))
                    .thenReturn(Optional.of(user));

            // 비밀번호 일치하도록 설정
            when(passwordEncoder.matches(rawPw, user.getPassword()))
                    .thenReturn(true);

            when(jwtTokenProvider.createAccessToken(user.getId(), user.getEmail()))
                    .thenReturn("ACCESS_TOKEN");
            when(jwtTokenProvider.createRefreshToken(user.getId()))
                    .thenReturn("REFRESH_TOKEN");

            // 기존 RT 없음
            when(refreshTokenRepository.findByUser(user))
                    .thenReturn(Optional.empty());

            // 저장할 때는 그냥 그대로 통과
            // (save 결과를 굳이 반환 안 써도 되면 stubbing 불필요)

            // when
            AuthService.Tokens tokens = authService.login(email, rawPw);

            // then
            assertThat(tokens.accessToken()).isEqualTo("ACCESS_TOKEN");
            assertThat(tokens.refreshToken()).isEqualTo("REFRESH_TOKEN");

            // RT가 저장됐는지 확인
            ArgumentCaptor<RefreshToken> captor =
                    ArgumentCaptor.forClass(RefreshToken.class);
            verify(refreshTokenRepository).save(captor.capture());

            RefreshToken saved = captor.getValue();
            assertThat(saved.getUser()).isEqualTo(user);
            assertThat(saved.getRefreshToken()).isEqualTo("REFRESH_TOKEN");
            assertThat(saved.getExpiresAt()).isAfter(LocalDateTime.now());
        }

        @Test
        @DisplayName("비밀번호 불일치 시 IllegalArgumentException 발생")
        void login_wrong_password() {
            // given
            String email = "p500wh@naver.com";
            String rawPw = "wrong";

            User user = dummyUser();

            when(userRepository.findByEmail(email))
                    .thenReturn(Optional.of(user));

            when(passwordEncoder.matches(rawPw, user.getPassword()))
                    .thenReturn(false);

            // when & then
            assertThatThrownBy(() -> authService.login(email, rawPw))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        @Test
        @DisplayName("존재하지 않는 이메일로 로그인 시 IllegalArgumentException 발생")
        void login_user_not_found() {
            // given
            String email = "no@naver.com";
            String rawPw = "12345678";

            when(userRepository.findByEmail(email))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> authService.login(email, rawPw))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("이메일 또는 비밀번호가 올바르지 않습니다.");
        }
    }

    @Nested
    @DisplayName("refresh() 테스트")
    class RefreshTests {

        @Test
        @DisplayName("정상적인 RT로 AT/RT 재발급 + RT 회전")
        void refresh_success() {
            // given
            String oldRt = "OLD_REFRESH_TOKEN";
            Long userId = 3L;
            User user = dummyUser();

            RefreshToken stored = RefreshToken.builder()
                    .user(user)
                    .refreshToken(oldRt)
                    .expiresAt(LocalDateTime.now().plusDays(10))
                    .build();

            when(jwtTokenProvider.getUserId(oldRt))
                    .thenReturn(userId);

            when(userRepository.findById(userId))
                    .thenReturn(Optional.of(user));

            when(refreshTokenRepository.findByUser(user))
                    .thenReturn(Optional.of(stored));

            when(jwtTokenProvider.createAccessToken(user.getId(), user.getEmail()))
                    .thenReturn("NEW_ACCESS");
            when(jwtTokenProvider.createRefreshToken(user.getId()))
                    .thenReturn("NEW_REFRESH");

            // when
            AuthService.Tokens tokens = authService.refresh(oldRt);

            // then
            assertThat(tokens.accessToken()).isEqualTo("NEW_ACCESS");
            assertThat(tokens.refreshToken()).isEqualTo("NEW_REFRESH");

            // 저장 호출 확인
            verify(refreshTokenRepository).save(stored);
            assertThat(stored.getRefreshToken()).isEqualTo("NEW_REFRESH");
            assertThat(stored.getExpiresAt()).isAfter(LocalDateTime.now());
        }

        @Test
        @DisplayName("DB에 저장된 RT와 presented RT가 다르면 삭제 + IllegalStateException")
        void refresh_mismatch_token() {
            // given
            String presentedRt = "PRESENTED";
            Long userId = 3L;
            User user = dummyUser();

            RefreshToken stored = RefreshToken.builder()
                    .user(user)
                    .refreshToken("OTHER_RT")
                    .expiresAt(LocalDateTime.now().plusDays(10))
                    .build();

            when(jwtTokenProvider.getUserId(presentedRt))
                    .thenReturn(userId);

            when(userRepository.findById(userId))
                    .thenReturn(Optional.of(user));

            when(refreshTokenRepository.findByUser(user))
                    .thenReturn(Optional.of(stored));

            // when & then
            assertThatThrownBy(() -> authService.refresh(presentedRt))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("리프레시 토큰 불일치");

            verify(refreshTokenRepository).deleteByUser(user);
        }

        @Test
        @DisplayName("DB에 RT가 없으면 IllegalArgumentException")
        void refresh_no_token_in_db() {
            // given
            String presentedRt = "PRESENTED";
            Long userId = 3L;
            User user = dummyUser();

            when(jwtTokenProvider.getUserId(presentedRt))
                    .thenReturn(userId);

            when(userRepository.findById(userId))
                    .thenReturn(Optional.of(user));

            when(refreshTokenRepository.findByUser(user))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> authService.refresh(presentedRt))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("리프레시 토큰이 없습니다.");
        }
    }

    @Nested
    @DisplayName("logout() 테스트")
    class LogoutTests {

        @Test
        @DisplayName("존재하는 userId로 logout 호출 시 해당 유저 RT 삭제")
        void logout_existing_user() {
            // given
            Long userId = 3L;
            User user = dummyUser();

            when(userRepository.findById(userId))
                    .thenReturn(Optional.of(user));

            // when
            authService.logout(userId);

            // then
            verify(refreshTokenRepository).deleteByUser(user);
        }

        @Test
        @DisplayName("없는 userId로 logout 호출 시 아무 일도 일어나지 않음")
        void logout_nonexistent_user() {
            // given
            Long userId = 999L;

            when(userRepository.findById(userId))
                    .thenReturn(Optional.empty());

            // when
            authService.logout(userId);

            // then
            verifyNoInteractions(refreshTokenRepository);
        }
    }
}

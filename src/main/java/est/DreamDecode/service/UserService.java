// src/main/java/est/DreamDecode/service/UserService.java
package est.DreamDecode.service;

import est.DreamDecode.domain.User;
import est.DreamDecode.dto.ResetPasswordRequest;
import est.DreamDecode.dto.SignupRequest;
import est.DreamDecode.repository.RefreshTokenRepository;
import est.DreamDecode.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public void signup(SignupRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        if (req.getBirthday().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("생년월일은 오늘 이후일 수 없습니다.");
        }

        validatePassword(req.getPassword());

        String hashed = passwordEncoder.encode(req.getPassword());

        User user = User.builder()
                .email(req.getEmail())
                .password(hashed)
                .name(req.getName())
                .birthday(req.getBirthday())
                .gender(req.getGender())
                .build();

        userRepository.save(user);
    }

    /** 비밀번호 재설정: 이메일 인증 코드 + 새 비밀번호 */
    @Transactional
    public void resetPassword(ResetPasswordRequest req) {
        boolean ok = emailVerificationService.consumePasswordResetOtp(req.getEmail(), req.getCode());
        if (!ok) {
            throw new IllegalArgumentException("인증번호가 올바르지 않거나 만료되었습니다.");
        }

        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("해당 이메일로 가입된 계정을 찾을 수 없습니다."));

        validatePassword(req.getNewPassword());

        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);

        // 기존 Refresh Token 삭제 → 다른 기기 모두 재로그인 필요
        refreshTokenRepository.deleteByUser(user);
    }

    /** 비밀번호 규칙 검증 (8~20자, 영문 + 숫자) */
    private void validatePassword(String pw) {
        if (pw == null || pw.length() < 8 || pw.length() > 20) {
            throw new IllegalArgumentException("비밀번호는 8~20자여야 합니다.");
        }
        if (!pw.matches("^(?=.*[A-Za-z])(?=.*\\d).+$")) {
            throw new IllegalArgumentException("비밀번호는 영문과 숫자를 모두 포함해야 합니다.");
        }
    }
}

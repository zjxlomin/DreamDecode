// src/main/java/est/DreamDecode/service/UserService.java
package est.DreamDecode.service;

import est.DreamDecode.domain.User;
import est.DreamDecode.dto.ResetPasswordRequest;
import est.DreamDecode.dto.SignupRequest;
import est.DreamDecode.dto.UpdateProfileRequest;
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

    // ================= 회원가입 =================
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

    // ================= 비밀번호 재설정 (이메일 인증 코드 기반) =================
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

    // ================= 내 정보 조회/수정 =================

    /** userId로 유저 조회 (없으면 예외) */
    @Transactional
    public User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }

    /** 내 정보 수정 (이름/성별/생년월일) */
    @Transactional
    public User updateProfile(Long userId, UpdateProfileRequest req) {
        if (req.birthday().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("생년월일은 오늘 이후일 수 없습니다.");
        }

        User user = getUser(userId);
        user.setName(req.name());
        user.setBirthday(req.birthday());
        user.setGender(req.gender());
        // save 호출 안 해도 트랜잭션 안에서 dirty checking으로 반영됨, 그래도 명시적으로 해도 무관
        return user;
    }

    // ================= 현재 비밀번호 확인 / 변경 =================

    /** 현재 비밀번호 확인용 */
    public boolean checkPassword(Long userId, String rawPassword) {
        User user = getUser(userId);
        return passwordEncoder.matches(rawPassword, user.getPassword());
    }

    /** 내 정보 페이지에서 비밀번호 변경 (현재 비밀번호 + 새 비밀번호) */
    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = getUser(userId);

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }

        // 비밀번호 규칙 재사용
        validatePassword(newPassword);

        String hashed = passwordEncoder.encode(newPassword);
        user.setPassword(hashed);

        // 비밀번호 변경 시에도, 기존 Refresh 토큰 다 날려서 재로그인 요구하는 게 안전
        refreshTokenRepository.deleteByUser(user);
    }

    // ================= 공통 비밀번호 검증 로직 =================

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

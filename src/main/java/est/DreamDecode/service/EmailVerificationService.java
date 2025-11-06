// src/main/java/est/DreamDecode/service/EmailVerificationService.java
package est.DreamDecode.service;

import est.DreamDecode.domain.EmailVerification;
import est.DreamDecode.domain.User;
import est.DreamDecode.domain.VerificationType;
import est.DreamDecode.repository.EmailVerificationRepository;
import est.DreamDecode.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final EmailVerificationRepository verificationRepository;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;
    private final BCryptPasswordEncoder passwordEncoder;

    private static final int OTP_TTL_MIN = 10;   // 유효시간 (분)
    private static final int MAX_ATTEMPTS = 5;   // 최대 시도 횟수
    private static final SecureRandom RANDOM = new SecureRandom();

    @Value("${app.mail.from:${spring.mail.username}}")
    private String fromAddress;

    /* ================= 회원가입 이메일 인증 ================= */

    /** 회원가입용 이메일 인증코드 발송 */
    @Transactional
    public void sendSignupOtp(User user) {
        String otp = generateOtp();
        String hash = passwordEncoder.encode(otp);

        Optional<EmailVerification> existing =
                verificationRepository.findByUserIdAndType(user.getId(), VerificationType.EMAIL_VERIFY);

        EmailVerification verification = existing.orElseGet(() ->
                EmailVerification.builder()
                        .userId(user.getId())
                        .type(VerificationType.EMAIL_VERIFY)
                        .emailCode(hash)
                        .expiresAt(LocalDateTime.now().plusMinutes(OTP_TTL_MIN))
                        .attempts(0)
                        .build()
        );

        verification.setEmailCode(hash);
        verification.setExpiresAt(LocalDateTime.now().plusMinutes(OTP_TTL_MIN));
        verification.setAttempts(0);
        verificationRepository.save(verification);

        sendMail(
                user.getEmail(),
                "[DreamDecode] 이메일 인증번호 안내",
                buildSignupHtml(otp)
        );
    }

    /** 이메일 + 코드 검증 (회원가입용) */
    @Transactional
    public boolean verifySignupOtp(String email, String code) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return false;

        // 이미 인증된 이메일이면 그냥 성공 처리
        if (user.getEmailVerifiedAt() != null) {
            return true;
        }

        EmailVerification v = verificationRepository
                .findByUserIdAndType(user.getId(), VerificationType.EMAIL_VERIFY)
                .orElse(null);
        if (v == null) return false;

        if (v.getExpiresAt().isBefore(LocalDateTime.now())) {
            verificationRepository.delete(v);
            return false;
        }

        if (v.getAttempts() != null && v.getAttempts() >= MAX_ATTEMPTS) {
            verificationRepository.delete(v);
            return false;
        }

        boolean ok = passwordEncoder.matches(code, v.getEmailCode());
        if (!ok) {
            v.setAttempts((v.getAttempts() == null ? 0 : v.getAttempts()) + 1);
            verificationRepository.save(v);
            return false;
        }

        user.setEmailVerifiedAt(LocalDateTime.now());
        userRepository.save(user);
        verificationRepository.delete(v);

        return true;
    }

    /* ================= 비밀번호 재설정용 이메일 인증 ================= */

    /** 비밀번호 재설정용 인증코드 발송 */
    @Transactional
    public void sendPasswordResetOtpByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("해당 이메일로 가입된 계정을 찾을 수 없습니다."));

        String otp = generateOtp();
        String hash = passwordEncoder.encode(otp);

        Optional<EmailVerification> existing =
                verificationRepository.findByUserIdAndType(user.getId(), VerificationType.PASSWORD_RESET);

        EmailVerification verification = existing.orElseGet(() ->
                EmailVerification.builder()
                        .userId(user.getId())
                        .type(VerificationType.PASSWORD_RESET)
                        .emailCode(hash)
                        .expiresAt(LocalDateTime.now().plusMinutes(OTP_TTL_MIN))
                        .attempts(0)
                        .build()
        );

        verification.setEmailCode(hash);
        verification.setExpiresAt(LocalDateTime.now().plusMinutes(OTP_TTL_MIN));
        verification.setAttempts(0);
        verificationRepository.save(verification);

        sendMail(
                user.getEmail(),
                "[DreamDecode] 비밀번호 재설정 인증번호 안내",
                buildPasswordResetHtml(otp)
        );
    }

    /**
     * 비밀번호 재설정용 코드 검증
     * - consume=false: 레코드 유지(verify-reset에서 사용)
     * - consume=true: 레코드 삭제(reset에서 사용)
     */
    @Transactional
    public boolean verifyPasswordResetOtp(String email, String code, boolean consume) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return false;

        EmailVerification v = verificationRepository
                .findByUserIdAndType(user.getId(), VerificationType.PASSWORD_RESET)
                .orElse(null);
        if (v == null) return false;

        if (v.getExpiresAt().isBefore(LocalDateTime.now())) {
            verificationRepository.delete(v);
            return false;
        }

        if (v.getAttempts() != null && v.getAttempts() >= MAX_ATTEMPTS) {
            verificationRepository.delete(v);
            return false;
        }

        boolean ok = passwordEncoder.matches(code, v.getEmailCode());
        if (!ok) {
            v.setAttempts((v.getAttempts() == null ? 0 : v.getAttempts()) + 1);
            verificationRepository.save(v);
            return false;
        }

        if (consume) {
            verificationRepository.delete(v);
        } else {
            // 성공했으면 시도 횟수는 0으로 돌려놓기 정도만
            v.setAttempts(0);
            verificationRepository.save(v);
        }

        return true;
    }

    /** 비밀번호 재설정 - 코드 "확인만" (verify-reset에서 사용) */
    @Transactional
    public boolean checkPasswordResetOtp(String email, String code) {
        return verifyPasswordResetOtp(email, code, false);
    }

    /** 비밀번호 재설정 - 코드 "소모" (reset 시 사용) */
    @Transactional
    public boolean consumePasswordResetOtp(String email, String code) {
        return verifyPasswordResetOtp(email, code, true);
    }

    /* ===== 내부 유틸 ===== */

    private String generateOtp() {
        int n = RANDOM.nextInt(1_000_000);
        return String.format("%06d", n);
    }

    private void sendMail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setFrom(fromAddress);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("이메일 발송 실패", e);
        }
    }

    private String buildSignupHtml(String otp) {
        return """
            <div style="font-family:Arial,sans-serif;max-width:560px;margin:auto">
              <h2>이메일 인증번호</h2>
              <p>아래 인증번호를 입력해 주세요.</p>
              <p style="font-size:24px;letter-spacing:3px;font-weight:bold;margin:20px 0">%s</p>
              <p>유효시간: %d분</p>
            </div>
            """.formatted(otp, OTP_TTL_MIN);
    }

    private String buildPasswordResetHtml(String otp) {
        return """
            <div style="font-family:Arial,sans-serif;max-width:560px;margin:auto">
              <h2>비밀번호 재설정 인증번호</h2>
              <p>아래 인증번호를 DreamDecode 비밀번호 재설정 화면에 입력해 주세요.</p>
              <p style="font-size:24px;letter-spacing:3px;font-weight:bold;margin:20px 0">%s</p>
              <p>유효시간: %d분</p>
            </div>
            """.formatted(otp, OTP_TTL_MIN);
    }
}

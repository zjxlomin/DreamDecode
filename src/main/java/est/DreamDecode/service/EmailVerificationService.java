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

    /**
     * 회원가입 직후 이메일 인증코드 발송
     */
    @Transactional
    public void sendSignupOtp(User user) {
        String otp = generateOtp();
        String hash = passwordEncoder.encode(otp);

        // 기존 행 있으면 갱신, 없으면 새로 생성
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

        sendMail(user.getEmail(), otp);
    }

    /**
     * 이메일 + 코드 검증
     * 성공 시 user.email_verified_at 갱신 및 인증 레코드 삭제
     */
    @Transactional
    public boolean verifySignupOtp(String email, String code) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return false;

        EmailVerification v = verificationRepository
                .findByUserIdAndType(user.getId(), VerificationType.EMAIL_VERIFY)
                .orElse(null);
        if (v == null) return false;

        // 만료 확인
        if (v.getExpiresAt().isBefore(LocalDateTime.now())) {
            verificationRepository.delete(v);
            return false;
        }

        // 시도 횟수 초과
        if (v.getAttempts() != null && v.getAttempts() >= MAX_ATTEMPTS) {
            verificationRepository.delete(v);
            return false;
        }

        // 코드 일치 확인 (BCrypt 비교)
        boolean ok = passwordEncoder.matches(code, v.getEmailCode());
        if (!ok) {
            v.setAttempts((v.getAttempts() == null ? 0 : v.getAttempts()) + 1);
            verificationRepository.save(v);
            return false;
        }

        // 성공 → 유저 인증 표시 + 레코드 삭제
        user.setEmailVerifiedAt(LocalDateTime.now());
        userRepository.save(user);
        verificationRepository.delete(v);

        return true;
    }

    /* ===== 내부 유틸 ===== */

    /** 6자리 숫자 코드 생성 */
    private String generateOtp() {
        int n = RANDOM.nextInt(1_000_000);
        return String.format("%06d", n);
    }

    /** 이메일 발송 (Gmail SMTP) */
    private void sendMail(String to, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setFrom(fromAddress);
            helper.setSubject("[DreamDecode] 이메일 인증번호 안내");
            helper.setText(buildHtml(otp), true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("이메일 발송 실패", e);
        }
    }

    /** HTML 템플릿 (간단 버전) */
    private String buildHtml(String otp) {
        return """
            <div style="font-family:Arial,sans-serif;max-width:560px;margin:auto">
              <h2>이메일 인증번호</h2>
              <p>아래 인증번호를 입력해 주세요.</p>
              <p style="font-size:24px;letter-spacing:3px;font-weight:bold;margin:20px 0">%s</p>
              <p>유효시간: %d분</p>
            </div>
            """.formatted(otp, OTP_TTL_MIN);
    }
}

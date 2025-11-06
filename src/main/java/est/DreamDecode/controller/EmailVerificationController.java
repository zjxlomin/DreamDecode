// src/main/java/est/DreamDecode/controller/EmailVerificationController.java
package est.DreamDecode.controller;

import est.DreamDecode.domain.User;
import est.DreamDecode.dto.EmailVerificationResponse;
import est.DreamDecode.dto.SendEmailRequest;
import est.DreamDecode.dto.VerifyEmailRequest;
import est.DreamDecode.repository.UserRepository;
import est.DreamDecode.service.EmailVerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/email")
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;
    private final UserRepository userRepository;

    /**
     * 회원가입용 인증코드 재발송
     * POST /api/email/resend-signup
     */
    @PostMapping("/resend-signup")
    public ResponseEntity<EmailVerificationResponse> resendSignup(@RequestBody @Valid SendEmailRequest req) {
        Optional<User> userOpt = userRepository.findByEmail(req.getEmail());
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new EmailVerificationResponse("존재하지 않는 이메일입니다."));
        }

        emailVerificationService.sendSignupOtp(userOpt.get());

        return ResponseEntity.ok(
                new EmailVerificationResponse("인증코드가 발송되었습니다.")
        );
    }

    /**
     * 회원가입 이메일 인증 코드 검증
     * POST /api/email/verify-signup
     */
    @PostMapping("/verify-signup")
    public ResponseEntity<EmailVerificationResponse> verifySignup(@RequestBody @Valid VerifyEmailRequest req) {
        boolean ok = emailVerificationService.verifySignupOtp(req.getEmail(), req.getCode());
        if (ok) {
            return ResponseEntity.ok(
                    new EmailVerificationResponse("인증이 완료되었습니다.")
            );
        }
        return ResponseEntity.badRequest()
                .body(new EmailVerificationResponse("인증에 실패했습니다."));
    }
}

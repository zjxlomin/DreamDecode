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

    /** 인증코드 메일 발송 */
    @PostMapping("/send")
    public ResponseEntity<EmailVerificationResponse> send(@RequestBody @Valid SendEmailRequest req) {
        Optional<User> userOpt = userRepository.findByEmail(req.getEmail());
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new EmailVerificationResponse("존재하지 않는 이메일입니다."));
        }

        emailVerificationService.sendSignupOtp(userOpt.get());

        return ResponseEntity.status(201)
                .body(new EmailVerificationResponse("인증코드가 발송되었습니다."));
    }

    /** 인증코드 검증 */
    @PostMapping("/verify")
    public ResponseEntity<EmailVerificationResponse> verify(@RequestBody @Valid VerifyEmailRequest req) {
        boolean ok = emailVerificationService.verifySignupOtp(req.getEmail(), req.getCode());
        if (ok) {
            return ResponseEntity.ok(new EmailVerificationResponse("인증이 완료되었습니다."));
        }
        return ResponseEntity.badRequest()
                .body(new EmailVerificationResponse("인증에 실패했습니다."));
    }
}

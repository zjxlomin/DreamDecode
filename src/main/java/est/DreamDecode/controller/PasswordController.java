// src/main/java/est/DreamDecode/controller/PasswordController.java
package est.DreamDecode.controller;

import est.DreamDecode.dto.EmailVerificationResponse;
import est.DreamDecode.dto.ResetPasswordRequest;
import est.DreamDecode.dto.SendEmailRequest;
import est.DreamDecode.dto.VerifyEmailRequest;
import est.DreamDecode.service.EmailVerificationService;
import est.DreamDecode.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/password")
public class PasswordController {

    private final EmailVerificationService emailVerificationService;
    private final UserService userService;

    /** 비밀번호 찾기 - 인증메일 보내기 */
    @PostMapping("/forgot")
    public ResponseEntity<EmailVerificationResponse> forgotPassword(
            @RequestBody @Valid SendEmailRequest req
    ) {
        emailVerificationService.sendPasswordResetOtpByEmail(req.getEmail());
        return ResponseEntity.ok(
                new EmailVerificationResponse("비밀번호 재설정 인증 메일을 발송했습니다.")
        );
    }

    /** 비밀번호 찾기 - 인증번호 검증 (1단계: 비번 변경은 아직 안 함) */
    @PostMapping("/verify-reset")
    public ResponseEntity<EmailVerificationResponse> verifyReset(
            @RequestBody @Valid VerifyEmailRequest req
    ) {
        boolean ok = emailVerificationService.checkPasswordResetOtp(req.getEmail(), req.getCode());
        if (!ok) {
            return ResponseEntity.badRequest()
                    .body(new EmailVerificationResponse("인증번호가 올바르지 않거나 만료되었습니다."));
        }
        return ResponseEntity.ok(
                new EmailVerificationResponse("이메일 인증이 완료되었습니다. 새 비밀번호를 입력해 주세요.")
        );
    }

    /** 비밀번호 재설정 (2단계: 코드 + 새 비밀번호로 실제 변경) */
    @PostMapping("/reset")
    public ResponseEntity<EmailVerificationResponse> resetPassword(
            @RequestBody @Valid ResetPasswordRequest req
    ) {
        userService.resetPassword(req);
        return ResponseEntity.ok(
                new EmailVerificationResponse("비밀번호가 성공적으로 변경되었습니다.")
        );
    }
}

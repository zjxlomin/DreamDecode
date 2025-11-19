// src/main/java/est/DreamDecode/controller/EmailVerificationController.java
package est.DreamDecode.controller;

import est.DreamDecode.domain.User;
import est.DreamDecode.dto.EmailVerificationResponse;
import est.DreamDecode.dto.SendEmailRequest;
import est.DreamDecode.dto.VerifyEmailRequest;
import est.DreamDecode.repository.UserRepository;
import est.DreamDecode.service.EmailVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/email")
@Tag(name = "이메일 인증 API", description = "회원가입 이메일 인증 관련 API (인증 불필요)")
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;
    private final UserRepository userRepository;

    /**
     * 회원가입용 인증코드 재발송
     * POST /api/email/resend-signup
     */
    @Operation(summary = "이메일 인증코드 재발송", description = "회원가입 시 발송된 이메일 인증코드를 재발송합니다. 인증 불필요.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "인증코드 재발송 성공"),
            @ApiResponse(responseCode = "400", description = "존재하지 않는 이메일")
    })
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
    @Operation(summary = "이메일 인증코드 검증", description = "회원가입 시 발송된 이메일 인증코드를 검증합니다. 인증 완료 후 로그인이 가능합니다. 인증 불필요.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "인증 성공"),
            @ApiResponse(responseCode = "400", description = "인증 실패")
    })
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
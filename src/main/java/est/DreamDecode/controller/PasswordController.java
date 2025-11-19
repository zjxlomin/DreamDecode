// src/main/java/est/DreamDecode/controller/PasswordController.java
package est.DreamDecode.controller;

import est.DreamDecode.dto.EmailVerificationResponse;
import est.DreamDecode.dto.ResetPasswordRequest;
import est.DreamDecode.dto.SendEmailRequest;
import est.DreamDecode.dto.VerifyEmailRequest;
import est.DreamDecode.service.EmailVerificationService;
import est.DreamDecode.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/password")
@Tag(name = "비밀번호 재설정 API", description = "비밀번호 찾기 및 재설정 관련 API (인증 불필요)")
public class PasswordController {

    private final EmailVerificationService emailVerificationService;
    private final UserService userService;

    /** 비밀번호 찾기 - 인증메일 보내기 */
    @Operation(summary = "비밀번호 재설정 인증메일 발송", description = "비밀번호를 잊어버린 경우 이메일로 인증 코드를 발송합니다. 인증 불필요.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "인증 메일 발송 성공"),
            @ApiResponse(responseCode = "400", description = "존재하지 않는 이메일")
    })
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
    @Operation(summary = "비밀번호 재설정 인증번호 검증", description = "이메일로 받은 인증번호를 검증합니다. 검증 성공 후 비밀번호 재설정이 가능합니다. 인증 불필요.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "인증 성공"),
            @ApiResponse(responseCode = "400", description = "인증번호 불일치 또는 만료")
    })
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
    @Operation(summary = "비밀번호 재설정", description = "인증번호와 새 비밀번호를 입력하여 비밀번호를 재설정합니다. 인증 불필요.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "비밀번호 재설정 성공"),
            @ApiResponse(responseCode = "400", description = "인증번호 불일치, 만료, 또는 새 비밀번호 검증 실패")
    })
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

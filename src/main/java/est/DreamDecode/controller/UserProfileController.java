// src/main/java/est/DreamDecode/controller/UserProfileController.java
package est.DreamDecode.controller;

import est.DreamDecode.domain.User;
import est.DreamDecode.dto.*;
import est.DreamDecode.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/me")
public class UserProfileController {

    private final UserService userService;

    private Long getUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalArgumentException("인증 정보가 없습니다.");
        }
        return (Long) authentication.getPrincipal(); // JwtTokenFilter 에서 넣어준 userId
    }

    /** 내 정보 조회 */
    @GetMapping
    public ResponseEntity<UserProfileResponse> getMe(Authentication authentication) {
        Long userId = getUserId(authentication);
        User user = userService.getUser(userId);
        return ResponseEntity.ok(UserProfileResponse.from(user));
    }

    /** 내 정보 수정 (이름/성별/생년월일) */
    @PutMapping
    public ResponseEntity<UserProfileResponse> updateMe(
            Authentication authentication,
            @RequestBody @Valid UpdateProfileRequest body
    ) {
        Long userId = getUserId(authentication);
        User updated = userService.updateProfile(userId, body);
        return ResponseEntity.ok(UserProfileResponse.from(updated));
    }

    /** 현재 비밀번호 확인 */
    @PostMapping("/check-password")
    public ResponseEntity<EmailVerificationResponse> checkPassword(
            Authentication authentication,
            @RequestBody @Valid CheckPasswordRequest body
    ) {
        Long userId = getUserId(authentication);
        boolean ok = userService.checkPassword(userId, body.currentPassword());
        if (!ok) {
            return ResponseEntity.badRequest()
                    .body(new EmailVerificationResponse("현재 비밀번호가 일치하지 않습니다."));
        }
        return ResponseEntity.ok(
                new EmailVerificationResponse("비밀번호가 확인되었습니다.")
        );
    }

    /** 비밀번호 변경 */
    @PostMapping("/change-password")
    public ResponseEntity<EmailVerificationResponse> changePassword(
            Authentication authentication,
            @RequestBody @Valid ChangePasswordRequest body
    ) {
        Long userId = getUserId(authentication);
        userService.changePassword(userId, body.currentPassword(), body.newPassword());
        return ResponseEntity.ok(
                new EmailVerificationResponse("비밀번호가 변경되었습니다.")
        );
    }
}

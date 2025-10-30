// src/main/java/est/DreamDecode/controller/UserController.java
package est.DreamDecode.controller;

import est.DreamDecode.dto.SignupRequest;
import est.DreamDecode.dto.SignupResponse;
import est.DreamDecode.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<SignupResponse> signup(@RequestBody @Valid SignupRequest req) {

        userService.signup(req);

        return ResponseEntity
                .status(201)
                .body(new SignupResponse( "회원가입이 완료되었습니다. 이메일 인증을 진행해 주세요."));
    }
}

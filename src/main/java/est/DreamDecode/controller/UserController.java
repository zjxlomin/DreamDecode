// src/main/java/est/DreamDecode/controller/UserController.java
package est.DreamDecode.controller;

import est.DreamDecode.dto.SignupRequest;
import est.DreamDecode.dto.SignupResponse;
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
@RequestMapping("/api/users")
@Tag(name = "사용자 API", description = "회원가입 관련 API")
public class UserController {
    private final UserService userService;

    @Operation(summary = "회원가입", description = "새로운 사용자를 등록합니다. 회원가입 후 이메일 인증이 필요합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "회원가입 성공"),
            @ApiResponse(responseCode = "400", description = "입력값 검증 실패 또는 이메일 중복")
    })
    @PostMapping("/signup")
    public ResponseEntity<SignupResponse> signup(@RequestBody @Valid SignupRequest req) {

        userService.signup(req);

        return ResponseEntity
                .status(201)
                .body(new SignupResponse( "회원가입이 완료되었습니다. 이메일 인증을 진행해 주세요."));
    }
}

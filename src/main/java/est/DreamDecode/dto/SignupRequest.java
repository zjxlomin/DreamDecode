package est.DreamDecode.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class SignupRequest {

    @Email
    @NotBlank(message = "이메일은 필수입니다.")
    private String email;

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 8, max = 64, message = "비밀번호는 8~64자여야 합니다.")
    private String password;

    @NotBlank(message = "이름은 필수입니다.")
    @Size(max = 50, message = "이름은 50자 이내여야 합니다.")
    private String name;

    @NotNull(message = "생년월일은 필수입니다.")
    private LocalDate birthday;

    @NotNull(message = "성별은 필수입니다.")
    private Integer gender;
    // 예시: 0=남, 1=여, 2=기타 (명시적으로 약속해두면 좋아요)
}

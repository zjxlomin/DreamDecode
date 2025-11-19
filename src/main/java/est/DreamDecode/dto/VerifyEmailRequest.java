package est.DreamDecode.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class VerifyEmailRequest {
    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String code;
}

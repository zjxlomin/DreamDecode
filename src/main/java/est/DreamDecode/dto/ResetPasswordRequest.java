// src/main/java/est/DreamDecode/dto/ResetPasswordRequest.java
package est.DreamDecode.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ResetPasswordRequest {

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String code;

    @NotBlank
    private String newPassword;
}

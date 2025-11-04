// src/main/java/est/DreamDecode/dto/auth/LoginRequest.java
package est.DreamDecode.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @Email @NotBlank String email,
        @NotBlank String password
) {}

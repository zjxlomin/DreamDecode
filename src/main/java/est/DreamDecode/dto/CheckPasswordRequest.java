package est.DreamDecode.dto;

import jakarta.validation.constraints.NotBlank;

public record CheckPasswordRequest(
        @NotBlank String currentPassword
) {}

// src/main/java/est/DreamDecode/dto/UpdateProfileRequest.java
package est.DreamDecode.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;

import java.time.LocalDate;

public record UpdateProfileRequest(
        @NotBlank String name,
        @NotNull @Past LocalDate birthday,
        @NotNull Integer gender
) {}

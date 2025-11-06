// src/main/java/est/DreamDecode/dto/UserProfileResponse.java
package est.DreamDecode.dto;

import est.DreamDecode.domain.User;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record UserProfileResponse(
        Long id,
        String email,
        String name,
        LocalDate birthday,
        Integer gender,
        LocalDateTime emailVerifiedAt,
        LocalDateTime createdAt
) {
    public static UserProfileResponse from(User u) {
        return new UserProfileResponse(
                u.getId(),
                u.getEmail(),
                u.getName(),
                u.getBirthday(),
                u.getGender(),
                u.getEmailVerifiedAt(),
                u.getCreatedAt()
        );
    }
}

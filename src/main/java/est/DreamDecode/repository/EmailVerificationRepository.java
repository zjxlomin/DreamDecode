// src/main/java/est/DreamDecode/repository/EmailVerificationRepository.java
package est.DreamDecode.repository;

import est.DreamDecode.domain.EmailVerification;
import est.DreamDecode.domain.VerificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {
    Optional<EmailVerification> findByUserIdAndType(Long userId, VerificationType type);
    long deleteByExpiresAtBefore(LocalDateTime now);
}

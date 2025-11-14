// src/main/java/est/DreamDecode/repository/RefreshTokenRepository.java
package est.DreamDecode.repository;

import est.DreamDecode.domain.RefreshToken;
import est.DreamDecode.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByUser(User user);
    void deleteByUser(User user);
}

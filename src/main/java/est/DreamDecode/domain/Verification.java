package est.DreamDecode.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "verifications")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Verification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "verification_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 32)
    private String type;  // email_verify / password_reset

    @Column(name = "email_token", nullable = false, length = 255)
    private String emailToken;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "consumed_at")
    private LocalDateTime consumedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;


    @Builder
    public Verification(User user, String type, String emailToken, LocalDateTime expiresAt) {
        this.user = user;
        this.type = type;
        this.emailToken = emailToken;
        this.expiresAt = expiresAt;
    }

    public boolean isUsed() {
        return consumedAt != null;
    }
}

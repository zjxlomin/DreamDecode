package est.DreamDecode.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "email_verifications")
public class EmailVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "email_verification_id")
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 32)
    private VerificationType type; // EMAIL_VERIFY / PASSWORD_RESET

    // OTP는 평문 저장 금지 → 해시만 저장
    @Column(name = "email_code", nullable = false, length = 255)
    private String emailCode;

    // 유효기간 (예: 발급 시각 + 10분)
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    // 시도 횟수 (최대 허용치는 백엔드 상수로 관리: 예) 5)
    @Column(name = "attempts", nullable = false)
    private Integer attempts = 0;


    @Builder
    public EmailVerification(Long userId, VerificationType type, String emailCode, LocalDateTime expiresAt, Integer attempts) {
        this.userId = userId;
        this.type = type;
        this.emailCode = emailCode;
        this.expiresAt = expiresAt;
        this.attempts = 0;
    }

}

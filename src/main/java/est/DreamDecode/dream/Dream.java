package est.DreamDecode.dream;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "dreams")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Dream {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "dream_id")
  private Long id;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "title", nullable = false)
  private String title;

  @Column(name = "content", nullable = false)
  private String content;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "is_public", nullable = false)
  private boolean isPublic;

  @Builder
  public Dream(Long userId, String title, String content, boolean isPublic) {
    this.userId = userId;
    this.title = title;
    this.content = content;
    this.isPublic = isPublic;
  }
}
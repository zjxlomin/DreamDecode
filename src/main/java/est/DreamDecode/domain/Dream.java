package est.DreamDecode.domain;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

  @Column(name = "categories", columnDefinition = "TEXT")
  private String categories;

  @Column(name = "tags", columnDefinition = "TEXT")
  private String tags;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "published", nullable = false)
  private boolean published;

    @OneToOne(mappedBy = "dream", cascade = CascadeType.REMOVE, fetch = FetchType.LAZY)
    private Analysis analysis;

    @OneToMany(mappedBy = "dream", cascade = CascadeType.REMOVE, fetch = FetchType.LAZY)
    private List<Scene> scenes = new ArrayList<>();

  @Builder
  public Dream(Long userId, String title, String content, String categories, String tags, boolean published) {
    this.userId = userId;
    this.title = title;
    this.content = content;
    this.categories = categories;
    this.tags = tags;
    this.published = published;
  }
}
package est.DreamDecode.dto;

import est.DreamDecode.domain.Dream;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class DreamRequest {
  private Long id;
  private Long userId;
  private String title;
  private String content;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private boolean isPublic;

  public Dream toEntity() {
    Dream dream = Dream.builder()
                          .userId(this.getUserId())
                          .title(this.getTitle())
                          .content(this.getContent())
                          .isPublic(this.isPublic())
                          .build();
    return dream;
  }
}

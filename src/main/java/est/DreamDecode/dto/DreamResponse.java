package est.DreamDecode.dto;

import est.DreamDecode.domain.Dream;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class DreamResponse {
  private Long id;
  private Long userId;
  private String title;
  private String content;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private boolean isPublic;

  public static DreamResponse from(Dream dream) {
    DreamResponse response = new DreamResponse();
    response.setId(dream.getId());
    response.setUserId(dream.getUserId());
    response.setTitle(dream.getTitle());
    response.setContent(dream.getContent());
    response.setCreatedAt(dream.getCreatedAt());
    response.setUpdatedAt(dream.getUpdatedAt());

    return  response;
  }
}

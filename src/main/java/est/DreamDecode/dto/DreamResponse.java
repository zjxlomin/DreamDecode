package est.DreamDecode.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import est.DreamDecode.domain.Dream;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class DreamResponse {
  private Long id;
  private Long userId;
  private String title;
  private String content;
  private List<String> categories;
  private List<String> tags;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private boolean published;

  public static DreamResponse from(Dream dream) {
    DreamResponse response = new DreamResponse();
    response.setId(dream.getId());
    response.setUserId(dream.getUserId());
    response.setTitle(dream.getTitle());
    response.setContent(dream.getContent());
    response.setCategories(dream.getCategories() != null && !dream.getCategories().trim().isEmpty() ?
                          Arrays.stream(dream.getCategories().split(","))
                                .map(String::trim)
                                .collect(Collectors.toList()) : List.of());
    response.setTags(dream.getTags() != null && !dream.getTags().trim().isEmpty() ?
                    Arrays.stream(dream.getTags().split(","))
                          .map(String::trim)
                          .collect(Collectors.toList()) : List.of());
    response.setCreatedAt(dream.getCreatedAt());
    response.setUpdatedAt(dream.getUpdatedAt());
    response.setPublished(dream.isPublished());

    return  response;
  }
}

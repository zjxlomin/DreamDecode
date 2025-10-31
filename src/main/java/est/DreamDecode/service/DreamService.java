package est.DreamDecode.service;

import est.DreamDecode.domain.Dream;
import est.DreamDecode.dto.DreamRequest;
import est.DreamDecode.dto.DreamResponse;
import est.DreamDecode.repository.DreamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DreamService {
  private DreamRepository dreamRepository;

  public DreamService(DreamRepository dreamRepository) {
    this.dreamRepository = dreamRepository;
  }

  public List<DreamResponse> getAllPublicDreams() {
    List<DreamResponse> dreams = dreamRepository.findAllByPublishedTrue().stream()
                                         .map(DreamResponse::from)
                                         .toList();
    return dreams;
  }

  public DreamResponse getDreamById(Long id) {
    Dream dream = dreamRepository.findById(id)
                         .orElseThrow(() -> new RuntimeException("Dream not found with id " + id));
    return DreamResponse.from(dream);
  }

  public Dream saveDream(DreamRequest request) {
    return dreamRepository.save(request.toEntity());
  }

  @Transactional
  public Dream updateDream(Long dreamId, DreamRequest request) {
    Dream dream = dreamRepository.findById(dreamId)
                          .orElseThrow(() -> new RuntimeException("Dream not found with id " + dreamId));

    // DTO에서 가져온 값으로 엔티티 업데이트
    if (request.getTitle() != null) {
      dream.setTitle(request.getTitle());
    }
    if (request.getContent() != null) {
      dream.setContent(request.getContent());
    }
    if (request.getUserId() != null) {
      dream.setUserId(request.getUserId());
    }
    dream.setPublished(request.isPublished());

    return dream;
  }

  public void deleteDream(Long dreamId) {
    dreamRepository.deleteById(dreamId);
  }
}

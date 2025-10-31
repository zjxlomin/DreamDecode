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
  private final DreamRepository dreamRepository;
  private final NaturalLanguageService nlpService;

  public DreamService(DreamRepository dreamRepository,  NaturalLanguageService nlpService) {
    this.dreamRepository = dreamRepository;
    this.nlpService = nlpService;
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

  @Transactional
  public Dream saveDream(DreamRequest request) {
    Dream dream = dreamRepository.save(request.toEntity());
    // Alan api로 꿈 내용 전송
    // String anlatext = request.getContent();
    // Alan api에 alantext 전달
    // Analysis analysis = 전송받은 json 엔티티로 변경하는 코드
    String nlptext = "전송받은 json 에서 emotion_summary 추출";
    nlpService.analyzeSentiment(nlptext);

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

package est.DreamDecode.service;

import est.DreamDecode.domain.Dream;
import est.DreamDecode.dto.DreamRequest;
import est.DreamDecode.dto.DreamResponse;
import est.DreamDecode.repository.DreamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

import static org.springframework.boot.origin.Origin.from;

@Service
public class DreamService {
  private final DreamRepository dreamRepository;
  private final NaturalLanguageService nlpService;
    private final AnalysisService analysisService;

  public DreamService(DreamRepository dreamRepository,  NaturalLanguageService nlpService,  AnalysisService analysisService) {
    this.dreamRepository = dreamRepository;
    this.nlpService = nlpService;
      this.analysisService = analysisService;
  }

  public List<DreamResponse> getAllPublicDreams() {
    List<DreamResponse> dreams = dreamRepository.findAllByPublishedTrue().stream()
                                         .map(DreamResponse::from)
                                         .toList();
    return dreams;
  }

  public List<DreamResponse> getDreamsByCategory(String category) {
    List<DreamResponse> dreams = dreamRepository.findByCategoriesContaining(category).stream()
                                         .map(DreamResponse::from)
                                         .toList();
    return dreams;
  }

  public List<DreamResponse> getDreamsByTag(String tag) {
    List<DreamResponse> dreams = dreamRepository.findByTagsContaining(tag).stream()
                                         .map(DreamResponse::from)
                                         .toList();
    return dreams;
  }

  public List<DreamResponse> getDreamsByTitle(String title) {
    List<DreamResponse> dreams = dreamRepository.findByTitleContaining(title).stream()
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

    Long dreamId = dream.getId();
    analysisService.addOrUpdateAnalysis(dreamId, true);

    // Alan api로 꿈 내용 전송
    // String anlatext = request.getContent();
    // Alan api에 alantext 전달
    // Analysis analysis = 전송받은 json 엔티티로 변경하는 코드
    String nlptext = "전송받은 json 에서 emotion_summary 추출";
    nlpService.analyzeSentiment(nlptext);

    return dream;
  }

  @Transactional
  public DreamResponse updateDream(Long dreamId, DreamRequest request) {
    Dream dream = dreamRepository.findById(dreamId)
                          .orElseThrow(() -> new RuntimeException("Dream not found with id " + dreamId));

    boolean contentUpdated = !dream.getContent().equals(request.getContent());

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

    if(contentUpdated) {
        analysisService.addOrUpdateAnalysis(dreamId, false);
    }

    return DreamResponse.from(dream);
  }

  public void deleteDream(Long dreamId) {
    dreamRepository.deleteById(dreamId);
  }
}

package est.DreamDecode.controller;

import com.google.cloud.language.v1.Sentiment;
import est.DreamDecode.domain.Dream;
import est.DreamDecode.dto.DreamRequest;
import est.DreamDecode.dto.DreamResponse;
import est.DreamDecode.dto.SentimentResult;
import est.DreamDecode.service.DreamService;
import est.DreamDecode.service.NaturalLanguageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class DreamController {
  private DreamService dreamService;
  private NaturalLanguageService nlpService;

  @Autowired
  public DreamController(DreamService dreamService, NaturalLanguageService nlpService) {
    this.dreamService = dreamService;
    this.nlpService = nlpService;
  }

  @GetMapping("/dream")
  public String getPublicDreams(Model model) {
    List<DreamResponse> dreams = dreamService.getAllPublicDreams();
    model.addAttribute("dreams", dreams);

    return "dreams";
  }

  // 전체 조회
  @GetMapping("/api/dream")
  public ResponseEntity<List<DreamResponse>> getPublicDreams() {
    List<DreamResponse> dreams = dreamService.getAllPublicDreams();
    return ResponseEntity.ok(dreams); // 200 OK + JSON 반환
  }

  // 카테고리로 조회
  @GetMapping("/api/dream/category/{category}")
  public ResponseEntity<List<DreamResponse>> getDreamsByCategory(@PathVariable("category") String category) {
    List<DreamResponse> dreams = dreamService.getDreamsByCategory(category);
    return ResponseEntity.ok(dreams); // 200 OK + JSON 반환
  }

  // 태그로 조회
  @GetMapping("/api/dream/tag/{tag}")
  public ResponseEntity<List<DreamResponse>> getDreamsByTag(@PathVariable("tag") String tag) {
    List<DreamResponse> dreams = dreamService.getDreamsByTag(tag);
    return ResponseEntity.ok(dreams); // 200 OK + JSON 반환
  }

  // 제목으로 조회
  @GetMapping("/api/dream/title")
  public ResponseEntity<List<DreamResponse>> getDreamsByTitle(@RequestParam("q") String title) {
    List<DreamResponse> dreams = dreamService.getDreamsByTitle(title);
    return ResponseEntity.ok(dreams); // 200 OK + JSON 반환
  }

  // 단일 조회
  @GetMapping("/api/dream/{id}")
  public ResponseEntity<DreamResponse> getDream(@PathVariable("id") Long dreamId) {
    DreamResponse dream = dreamService.getDreamById(dreamId);
    return ResponseEntity.ok(dream); // 200 OK + JSON 반환
  }

  // 등록
  @PostMapping("/api/dream")
  @ResponseBody
  public ResponseEntity<Dream> saveDream(@RequestBody DreamRequest request) {
    request.setUserId(3l);
    Dream savedDream = dreamService.saveDream(request);
    return ResponseEntity.status(201).body(savedDream);// 201 Created, 저장된 객체 반환
  }

  // 수정
  @PutMapping("/api/dream/{id}")
  @ResponseBody
  public DreamResponse updateDream(@PathVariable("id") Long dreamId, @RequestBody DreamRequest request) {
    return dreamService.updateDream(dreamId, request); // 200 OK + 업데이트된 객체 반환
  }

  // 삭제
  @DeleteMapping("/api/dream/{id}")
  @ResponseBody
  public ResponseEntity<Void> deleteDream(@PathVariable("id") Long dreamId) {
    dreamService.deleteDream(dreamId);
    return ResponseEntity.noContent().build(); // 204 No Content
  }

  // Google NLP API 테스트용 컨트롤러
  @GetMapping("/analyze-sentiment")
  @ResponseBody
  public String getSentiment(@RequestParam String text) {
    SentimentResult sentiment = nlpService.analyzeSentiment(text);

    if (sentiment == null) {
      return "감정 분석 결과를 찾을 수 없습니다.";
    }

    // Score: -1.0(부정) ~ 1.0(긍정)
    // Magnitude: 감정의 강도 (0.0 ~ 무한대, 길수록 높아짐)
    return String.format(
            "감정 점수(Score): %.3f \n 감정 강도(Magnitude): %.3f",
            sentiment.getScore(),
            sentiment.getMagnitude()
    );
  }
}

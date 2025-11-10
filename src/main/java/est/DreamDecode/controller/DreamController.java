package est.DreamDecode.controller;

import est.DreamDecode.domain.Dream;
import est.DreamDecode.dto.DreamRequest;
import est.DreamDecode.dto.DreamResponse;
import est.DreamDecode.service.DreamService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class DreamController {
  private final DreamService dreamService;

    private Long getUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalArgumentException("인증 정보가 없습니다.");
        }
        return (Long) authentication.getPrincipal(); // JwtTokenFilter 에서 넣어준 userId
    }

  @GetMapping("/dream")
  public String getPublicDreams(Model model) {
    // 초기 로드는 첫 페이지(9개)만
    Page<DreamResponse> dreamPage = dreamService.getAllPublicDreams(0);
    model.addAttribute("dreams", dreamPage.getContent());
    model.addAttribute("hasMore", dreamPage.hasNext());

    return "dreams";
  }

  // 전체 조회 (페이지네이션)
  @GetMapping("/api/dream")
  public ResponseEntity<Page<DreamResponse>> getPublicDreams(
      @RequestParam(value = "page", defaultValue = "0") int page) {
    Page<DreamResponse> dreamPage = dreamService.getAllPublicDreams(page);
    return ResponseEntity.ok(dreamPage); // 200 OK + JSON 반환
  }

  // 카테고리로 조회 (페이지네이션)
  @GetMapping("/api/dream/category/{category}")
  public ResponseEntity<Page<DreamResponse>> getDreamsByCategory(
      @PathVariable("category") String category,
      @RequestParam(value = "page", defaultValue = "0") int page) {
    Page<DreamResponse> dreamPage = dreamService.getDreamsByCategory(category, page);
    return ResponseEntity.ok(dreamPage); // 200 OK + JSON 반환
  }

  // 태그로 조회 (페이지네이션)
  @GetMapping("/api/dream/tag/{tag}")
  public ResponseEntity<Page<DreamResponse>> getDreamsByTag(
      @PathVariable("tag") String tag,
      @RequestParam(value = "page", defaultValue = "0") int page) {
    Page<DreamResponse> dreamPage = dreamService.getDreamsByTag(tag, page);
    return ResponseEntity.ok(dreamPage); // 200 OK + JSON 반환
  }

  // 제목으로 조회 (페이지네이션)
  @GetMapping("/api/dream/title")
  public ResponseEntity<Page<DreamResponse>> getDreamsByTitle(
      @RequestParam("q") String title,
      @RequestParam(value = "page", defaultValue = "0") int page) {
    Page<DreamResponse> dreamPage = dreamService.getDreamsByTitle(title, page);
    return ResponseEntity.ok(dreamPage); // 200 OK + JSON 반환
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
  public ResponseEntity<Dream> saveDream(@RequestBody DreamRequest request, Authentication authentication) {
    request.setUserId(getUserId(authentication));
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
}

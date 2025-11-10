package est.DreamDecode.controller;

import est.DreamDecode.domain.Dream;
import est.DreamDecode.dto.DreamRequest;
import est.DreamDecode.dto.DreamResponse;
import est.DreamDecode.service.DreamService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class DreamController {
  private final DreamService dreamService;

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
  public ResponseEntity<Dream> saveDream(@RequestBody DreamRequest request) {
    Long userId = getCurrentUserId();
    request.setUserId(userId);
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

  // 내가 쓴 꿈 기본(최신) 목록 - 기본 4개, limit 지정 가능
  @GetMapping("/api/dream/my")
  public ResponseEntity<List<DreamResponse>> getMyDreams(
      @RequestParam(value = "limit", required = false) Integer limit) {
    Long userId = getCurrentUserId();
    int resolvedLimit = (limit == null || limit <= 0) ? 4 : limit;
    List<DreamResponse> dreams = dreamService.getMyDreams(userId, resolvedLimit);
    return ResponseEntity.ok(dreams); // 200 OK + JSON 반환
  }

  // 내가 쓴 꿈 전체 조회
  @GetMapping("/api/dream/my/all")
  public ResponseEntity<List<DreamResponse>> getMyAllDreams() {
    Long userId = getCurrentUserId();
    List<DreamResponse> dreams = dreamService.getMyAllDreams(userId);
    return ResponseEntity.ok(dreams); // 200 OK + JSON 반환
  }

  private Long getCurrentUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || authentication.getPrincipal() == null) {
      throw new IllegalStateException("인증된 사용자를 찾을 수 없습니다.");
    }

    Object principal = authentication.getPrincipal();
    if (principal instanceof Long userId) {
      return userId;
    }
    if (principal instanceof String principalStr) {
      return Long.valueOf(principalStr);
    }
    throw new IllegalStateException("지원되지 않는 인증 주체 타입: " + principal.getClass());
  }
}

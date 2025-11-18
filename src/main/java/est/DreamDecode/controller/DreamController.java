package est.DreamDecode.controller;

import est.DreamDecode.domain.Dream;
import est.DreamDecode.dto.DreamRequest;
import est.DreamDecode.dto.DreamResponse;
import est.DreamDecode.service.DreamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "꿈 관리 API", description = "꿈 등록, 조회, 수정, 삭제, 검색 관련 API")
public class DreamController {
  private final DreamService dreamService;

  // 현재 로그인 된 사용자 확인
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

  // 전체 조회 (페이지네이션)
  @Operation(summary = "공개 꿈 목록 조회", description = "공개 설정된 모든 꿈을 페이지네이션으로 조회합니다. 인증 불필요.")
  @Parameter(name = "page", description = "페이지 번호 (0부터 시작)", example = "0")
  @ApiResponse(responseCode = "200", description = "조회 성공")
  @GetMapping("/api/dream")
  public ResponseEntity<Page<DreamResponse>> getPublicDreams(
      @RequestParam(value = "page", defaultValue = "0") int page) {
    Page<DreamResponse> dreamPage = dreamService.getAllPublicDreams(page);
    return ResponseEntity.ok(dreamPage); // 200 OK + JSON 반환
  }

  // 카테고리로 조회 (페이지네이션)
  @Operation(summary = "카테고리별 꿈 검색", description = "특정 카테고리로 필터링하여 꿈을 검색합니다. 인증 불필요.")
  @Parameter(name = "category", description = "카테고리명", example = "불안")
  @Parameter(name = "page", description = "페이지 번호 (0부터 시작)", example = "0")
  @ApiResponse(responseCode = "200", description = "조회 성공")
  @GetMapping("/api/dream/category/{category}")
  public ResponseEntity<Page<DreamResponse>> getDreamsByCategory(
      @PathVariable("category") String category,
      @RequestParam(value = "page", defaultValue = "0") int page) {
    Page<DreamResponse> dreamPage = dreamService.getDreamsByCategory(category, page);
    return ResponseEntity.ok(dreamPage); // 200 OK + JSON 반환
  }

  // 태그로 조회 (페이지네이션)
  @Operation(summary = "태그별 꿈 검색", description = "특정 태그로 필터링하여 꿈을 검색합니다. 인증 불필요.")
  @Parameter(name = "tag", description = "태그명", example = "하늘")
  @Parameter(name = "page", description = "페이지 번호 (0부터 시작)", example = "0")
  @ApiResponse(responseCode = "200", description = "조회 성공")
  @GetMapping("/api/dream/tag/{tag}")
  public ResponseEntity<Page<DreamResponse>> getDreamsByTag(
      @PathVariable("tag") String tag,
      @RequestParam(value = "page", defaultValue = "0") int page) {
    Page<DreamResponse> dreamPage = dreamService.getDreamsByTag(tag, page);
    return ResponseEntity.ok(dreamPage); // 200 OK + JSON 반환
  }

  // 제목으로 조회 (페이지네이션)
  @Operation(summary = "제목별 꿈 검색", description = "제목 키워드로 꿈을 검색합니다. 인증 불필요.")
  @Parameter(name = "title", description = "검색할 제목 키워드", example = "하늘")
  @Parameter(name = "page", description = "페이지 번호 (0부터 시작)", example = "0")
  @ApiResponse(responseCode = "200", description = "조회 성공")
  @GetMapping("/api/dream/title")
  public ResponseEntity<Page<DreamResponse>> getDreamsByTitle(
      @RequestParam("title") String title,
      @RequestParam(value = "page", defaultValue = "0") int page) {
    Page<DreamResponse> dreamPage = dreamService.getDreamsByTitle(title, page);
    return ResponseEntity.ok(dreamPage); // 200 OK + JSON 반환
  }

  // 단일 조회
  @Operation(summary = "꿈 상세 조회", description = "특정 꿈의 상세 정보와 AI 분석 결과를 조회합니다. 인증 불필요.")
  @Parameter(name = "id", description = "꿈 ID", example = "1")
  @ApiResponse(responseCode = "200", description = "조회 성공")
  @GetMapping("/api/dream/{id}")
  public ResponseEntity<DreamResponse> getDream(@PathVariable("id") Long dreamId) {
    DreamResponse dream = dreamService.getDreamById(dreamId);
    return ResponseEntity.ok(dream); // 200 OK + JSON 반환
  }

  // 등록
  @Operation(summary = "꿈 등록", description = "새로운 꿈을 등록합니다. 등록 시 자동으로 AI 분석이 수행됩니다. 인증 필요.")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "201", description = "등록 성공"),
          @ApiResponse(responseCode = "401", description = "인증 필요")
  })
  @PostMapping("/api/dream")
  @ResponseBody
  public ResponseEntity<Dream> saveDream(@RequestBody DreamRequest request) {
    Long userId = getCurrentUserId();
    request.setUserId(userId);
    Dream savedDream = dreamService.saveDream(request);
    return ResponseEntity.status(201).body(savedDream);// 201 Created, 저장된 객체 반환
  }

  // 수정
  @Operation(summary = "꿈 수정", description = "기존 꿈 정보를 수정합니다. 수정 시 자동으로 AI 분석이 재생성됩니다. 인증 필요.")
  @Parameter(name = "id", description = "꿈 ID", example = "1")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "수정 성공"),
          @ApiResponse(responseCode = "401", description = "인증 필요"),
          @ApiResponse(responseCode = "403", description = "본인의 꿈만 수정 가능")
  })
  @PutMapping("/api/dream/{id}")
  @ResponseBody
  public DreamResponse updateDream(@PathVariable("id") Long dreamId, @RequestBody DreamRequest request) {
    return dreamService.updateDream(dreamId, request); // 200 OK + 업데이트된 객체 반환
  }

  // 삭제
  @Operation(summary = "꿈 삭제", description = "본인이 작성한 꿈을 삭제합니다. 인증 필요.")
  @Parameter(name = "id", description = "꿈 ID", example = "1")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "204", description = "삭제 성공"),
          @ApiResponse(responseCode = "401", description = "인증 필요"),
          @ApiResponse(responseCode = "403", description = "본인의 꿈만 삭제 가능")
  })
  @DeleteMapping("/api/dream/{id}")
  @ResponseBody
  public ResponseEntity<Void> deleteDream(@PathVariable("id") Long dreamId) {
    dreamService.deleteDream(dreamId);
    return ResponseEntity.noContent().build(); // 204 No Content
  }

  // 내가 쓴 꿈 기본(최신) 목록 - 기본 4개, limit 지정 가능
  @Operation(summary = "내 꿈 목록 조회", description = "본인이 작성한 꿈 목록을 조회합니다. 기본 4개, limit 파라미터로 개수 조정 가능. 인증 필요.")
  @Parameter(name = "limit", description = "조회할 개수 (기본값: 4)", example = "10")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "조회 성공"),
          @ApiResponse(responseCode = "401", description = "인증 필요")
  })
  @GetMapping("/api/dream/my")
  public ResponseEntity<List<DreamResponse>> getMyDreams(
      @RequestParam(value = "limit", required = false) Integer limit) {
    Long userId = getCurrentUserId();
    int resolvedLimit = (limit == null || limit <= 0) ? 4 : limit;
    List<DreamResponse> dreams = dreamService.getMyDreams(userId, resolvedLimit);
    return ResponseEntity.ok(dreams); // 200 OK + JSON 반환
  }

  // 내가 쓴 꿈 전체 조회
  @Operation(summary = "내 꿈 전체 조회", description = "본인이 작성한 모든 꿈을 조회합니다. 인증 필요.")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "조회 성공"),
          @ApiResponse(responseCode = "401", description = "인증 필요")
  })
  @GetMapping("/api/dream/my/all")
  public ResponseEntity<List<DreamResponse>> getMyAllDreams() {
    Long userId = getCurrentUserId();
    List<DreamResponse> dreams = dreamService.getMyAllDreams(userId);
    return ResponseEntity.ok(dreams); // 200 OK + JSON 반환
  }

}

package est.DreamDecode.controller;

import est.DreamDecode.domain.Dream;
import est.DreamDecode.dto.DreamRequest;
import est.DreamDecode.dto.DreamResponse;
import est.DreamDecode.service.DreamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class DreamController {
  private DreamService dreamService;

  @Autowired
  public DreamController(DreamService dreamService) {
    this.dreamService = dreamService;
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
  public ResponseEntity<Dream> updateDream(@PathVariable("id") Long dreamId, @RequestBody DreamRequest request) {
    Dream updated = dreamService.updateDream(dreamId, request);
    return ResponseEntity.ok(updated); // 200 OK + 업데이트된 객체 반환
  }

  // 삭제
  @DeleteMapping("/api/dream/{id}")
  @ResponseBody
  public ResponseEntity<Void> deleteDream(@PathVariable("id") Long dreamId) {
    dreamService.deleteDream(dreamId);
    return ResponseEntity.noContent().build(); // 204 No Content
  }
}

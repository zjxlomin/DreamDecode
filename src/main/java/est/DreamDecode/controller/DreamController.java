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

  @GetMapping("/dreams")
  public String getPublicDreams(Model model) {
    List<DreamResponse> dreams = dreamService.getAllPublicDreams();
    model.addAttribute("dreams", dreams);

    return "dreams";
  }

  @PostMapping("/api/dreams")
  @ResponseBody
  public ResponseEntity<Dream> saveDream(@RequestBody DreamRequest request) {
    Dream savedDream = dreamService.saveDream(request);
    return ResponseEntity.status(201).body(savedDream);// 201 Created, 저장된 객체 반환
  }

  @PutMapping("/api/dreams/{id}")
  @ResponseBody
  public ResponseEntity<Dream> updateDream(@PathVariable("id") Long dreamId, @RequestBody DreamRequest request) {
    Dream updated = dreamService.updateDream(dreamId, request);
    return ResponseEntity.ok(updated); // 200 OK + 업데이트된 객체 반환
  }

  @DeleteMapping("api/dreams/{id}")
  @ResponseBody
  public ResponseEntity<Void> deleteDream(@PathVariable("id") Long dreamId) {
    dreamService.deleteDream(dreamId);
    return ResponseEntity.noContent().build(); // 204 No Content
  }
}

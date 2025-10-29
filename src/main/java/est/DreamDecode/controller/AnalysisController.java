package est.DreamDecode.controller;

import est.DreamDecode.domain.Analysis;
import est.DreamDecode.dto.AddAnalysisRequest;
import est.DreamDecode.dto.AnalysisResponse;
import est.DreamDecode.dto.UpdateAnalysisRequest;
import est.DreamDecode.service.AnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/analysis")
public class AnalysisController {
    private final AnalysisService analysisService;

    @PostMapping
    public ResponseEntity<Analysis> addAnalysis(@RequestBody AddAnalysisRequest request) {
        Analysis addAnalysis = analysisService.addAnalysis(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(addAnalysis);
    }

    @GetMapping("/{analysisId}")
    public ResponseEntity<AnalysisResponse> getAnalysisById(@PathVariable Long analysisId) {
        Analysis analysis = analysisService.getAnalysisById(analysisId);
        AnalysisResponse response = new AnalysisResponse(analysis);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{analysisId}")
    public ResponseEntity<Analysis> updateAnalysis(@PathVariable Long analysisId, @RequestBody UpdateAnalysisRequest request) {
        Analysis updateAnalysis = analysisService.updateAnalysis(analysisId, request);
        return ResponseEntity.status(HttpStatus.OK).body(updateAnalysis);
    }

    @DeleteMapping("/{analysisId}")
    public ResponseEntity<Void> deleteAnalysisById(@PathVariable Long analysisId) {
        analysisService.deleteAnalysisById(analysisId);
        return ResponseEntity.ok().build();
    }
}
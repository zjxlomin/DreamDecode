package est.DreamDecode.controller;

import est.DreamDecode.domain.Analysis;
import est.DreamDecode.dto.AnalysisResponse;
import est.DreamDecode.service.AnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dream")
public class AnalysisController {
    private final AnalysisService analysisService;

    @PostMapping("/{dreamId}/analysis")
    public AnalysisResponse addAnalysis(@PathVariable Long dreamId) {
        return analysisService.addOrUpdateAnalysis(dreamId, true);
    }

    @GetMapping("/{dreamId}/analysis")
    public ResponseEntity<AnalysisResponse> getAnalysisById(@PathVariable Long dreamId) {
        Analysis analysis = analysisService.getAnalysisByDreamId(dreamId);
        AnalysisResponse response = new AnalysisResponse(analysis);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{dreamId}/analysis")
    public AnalysisResponse updateAnalysis(@PathVariable Long dreamId) {
        return analysisService.addOrUpdateAnalysis(dreamId, false);
    }
}
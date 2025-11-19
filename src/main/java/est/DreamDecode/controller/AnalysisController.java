package est.DreamDecode.controller;

import est.DreamDecode.domain.Analysis;
import est.DreamDecode.dto.AnalysisResponse;
import est.DreamDecode.service.AnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dream")
@Tag(name = "꿈 분석 API", description = "꿈 AI 분석 생성 및 조회 관련 API")
public class AnalysisController {
    private final AnalysisService analysisService;

    @Operation(summary = "꿈 분석 수동 생성", description = "특정 꿈에 대한 AI 분석을 수동으로 생성합니다. 인증 필요.")
    @Parameter(name = "dreamId", description = "꿈 ID", example = "1")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "분석 생성 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @PostMapping("/{dreamId}/analysis")
    public AnalysisResponse addAnalysis(@PathVariable Long dreamId) {
        return analysisService.addOrUpdateAnalysis(dreamId, true);
    }

    @Operation(summary = "꿈 분석 조회", description = "특정 꿈의 AI 분석 결과를 조회합니다. 인증 불필요.")
    @Parameter(name = "dreamId", description = "꿈 ID", example = "1")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/{dreamId}/analysis")
    public ResponseEntity<AnalysisResponse> getAnalysisById(@PathVariable Long dreamId) {
        Analysis analysis = analysisService.getAnalysisByDreamId(dreamId);
        AnalysisResponse response = new AnalysisResponse(analysis);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "꿈 분석 수동 재생성", description = "기존 꿈 분석 결과를 다시 생성합니다. 인증 필요.")
    @Parameter(name = "dreamId", description = "꿈 ID", example = "1")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "분석 재생성 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @PutMapping("/{dreamId}/analysis")
    public AnalysisResponse updateAnalysis(@PathVariable Long dreamId) {
        return analysisService.addOrUpdateAnalysis(dreamId, false);
    }
}
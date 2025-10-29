package est.DreamDecode.analysis.dto;

import est.DreamDecode.analysis.Analysis;
import lombok.Getter;

@Getter
public class AnalysisResponse {
    private Long analysisId;
    private String analysisResult;
    private double sentiment;

    public AnalysisResponse(Analysis analysis) {
        this.analysisId = analysis.getAnalysisId();
        this.analysisResult = analysis.getAnalysisResult();
        this.sentiment = analysis.getSentiment();
    }
}

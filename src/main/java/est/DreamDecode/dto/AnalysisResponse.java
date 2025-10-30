package est.DreamDecode.dto;

import est.DreamDecode.domain.Analysis;
import est.DreamDecode.domain.Dream;
import lombok.Getter;

@Getter
public class AnalysisResponse {
    private Long analysisId;
    private String analysisResult;
    private double sentiment;
    private Dream dream;

    public AnalysisResponse(Analysis analysis) {
        this.analysisId = analysis.getAnalysisId();
        this.analysisResult = analysis.getAnalysisResult();
        this.sentiment = analysis.getSentiment();
        this.dream = analysis.getDream();
    }
}
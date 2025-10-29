package est.DreamDecode.analysis.dto;

import lombok.Getter;

@Getter
public class UpdateAnalysisRequest {
    private String analysisResult;
    private double sentiment;

    public UpdateAnalysisRequest(String analysisResult, double sentiment) {
        this.analysisResult = analysisResult;
        this.sentiment = sentiment;
    }
}

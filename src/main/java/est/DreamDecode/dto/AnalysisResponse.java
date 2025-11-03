package est.DreamDecode.dto;

import est.DreamDecode.domain.Analysis;
import est.DreamDecode.domain.Dream;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class AnalysisResponse {
    private Long analysisId;
    private String insight;
    private String suggestion;
    private String categories;
    private String tags;
    private String summary;
    private double sentiment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Dream dream;

    public AnalysisResponse(Analysis analysis) {
        this.analysisId = analysis.getAnalysisId();
        this.insight = analysis.getInsight();
        this.suggestion = analysis.getSuggestion();
        this.categories = analysis.getCategories();
        this.tags = analysis.getTags();
        this.summary = analysis.getSummary();
        this.sentiment = analysis.getSentiment();
        this.createdAt = analysis.getCreatedAt();
        this.updatedAt = analysis.getUpdatedAt();
        this.dream = analysis.getDream();
    }
}
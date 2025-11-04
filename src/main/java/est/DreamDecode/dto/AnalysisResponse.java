package est.DreamDecode.dto;

import est.DreamDecode.domain.Analysis;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class AnalysisResponse {
    private Long analysisId;
    private List<SceneResponse> scenes;
    private String insight;
    private String suggestion;
    private String categories;
    private String tags;
    private String summary;
    private double sentiment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public AnalysisResponse(Analysis analysis) {
        this.analysisId = analysis.getAnalysisId();
        this.scenes = analysis.getDream().getScenes()
                .stream().map(SceneResponse::new).toList();
        this.insight = analysis.getInsight();
        this.suggestion = analysis.getSuggestion();
        this.categories = analysis.getCategories();
        this.tags = analysis.getTags();
        this.summary = analysis.getSummary();
        this.sentiment = analysis.getSentiment();
        this.createdAt = analysis.getCreatedAt();
        this.updatedAt = analysis.getUpdatedAt();
    }
}
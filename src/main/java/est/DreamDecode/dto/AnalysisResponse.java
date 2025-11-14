package est.DreamDecode.dto;

import est.DreamDecode.domain.Analysis;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class AnalysisResponse {
    private Long analysisId;
    private Long dreamId;
    private Long userId;
    private String dreamTitle;
    private String dreamContent;
    private boolean dreamPublished;
    private List<SceneResponse> scenes;
    private String insight;
    private String suggestion;
    private String categories;
    private String tags;
    private String summary;
    private double sentiment;
    private double magnitude;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public AnalysisResponse(Analysis analysis) {
        this.analysisId = analysis.getAnalysisId();
        this.dreamId = analysis.getDream().getId();
        this.userId = analysis.getDream().getUserId();
        this.dreamTitle = analysis.getDream().getTitle();
        this.dreamContent = analysis.getDream().getContent();
        this.dreamPublished = analysis.getDream().isPublished();
        this.scenes = analysis.getDream().getScenes()
                .stream().map(SceneResponse::new).toList();
        this.insight = analysis.getInsight();
        this.suggestion = analysis.getSuggestion();
        this.categories = analysis.getDream().getCategories();
        this.tags = analysis.getDream().getTags();
        this.summary = analysis.getSummary();
        this.sentiment = analysis.getSentiment();
        this.magnitude = analysis.getMagnitude();
        this.createdAt = analysis.getCreatedAt();
        this.updatedAt = analysis.getUpdatedAt();
    }
}
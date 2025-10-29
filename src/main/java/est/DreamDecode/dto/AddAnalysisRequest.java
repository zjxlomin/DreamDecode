package est.DreamDecode.dto;


import est.DreamDecode.domain.Analysis;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class AddAnalysisRequest {
    private String analysisResult;
    private double sentiment;

    public Analysis toEntity(){
        return Analysis.builder()
                .analysisResult(analysisResult)
                .sentiment(sentiment)
                .build();
    }

}


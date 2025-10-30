package est.DreamDecode.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "analysis")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Analysis {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "analysis", updatable = false)
    private Long analysisId;

    @Column(name = "analysis_result", nullable = false)
    private String analysisResult;

    @Column(name = "sentiment", nullable = false)
    private double sentiment;

    @OneToOne
    @JoinColumn(name = "dream_id")
    private Dream dream;

    @Builder
    public Analysis(String analysisResult, double sentiment) {
        this.analysisResult = analysisResult;
        this.sentiment = sentiment;
    }

    public void updateAnalysis(String analysisResult, double sentiment) {
        this.analysisResult = analysisResult;
        this.sentiment = sentiment;
    }
}
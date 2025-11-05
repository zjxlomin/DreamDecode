package est.DreamDecode.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "analysis")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Analysis {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "analysis_id", updatable = false)
    private Long analysisId;

    @Column(name = "insight", nullable = false)
    private String insight;

    @Column(name = "suggestion", nullable = false)
    private String suggestion;

    @Column(name = "summary", nullable = false)
    private String summary;

    @Column(name = "sentiment", nullable = false)
    private double sentiment;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToOne
    @JoinColumn(name = "dream_id")
    private Dream dream;

    public void updateAnalysis(
            String insight,
            String suggestion,
            String summary,
            double sentiment
    ) {
        this.insight = insight;
        this.suggestion = suggestion;
        this.summary = summary;
        this.sentiment = sentiment;
    }
}
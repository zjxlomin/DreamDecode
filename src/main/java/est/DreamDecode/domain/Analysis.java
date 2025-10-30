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
    @Column(name = "analysis", updatable = false)
    private Long analysisId;

    @Column(name = "analysis_result", nullable = false)
    private String analysisResult;

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

    public void updateAnalysis(String analysisResult, double sentiment) {
        this.analysisResult = analysisResult;
        this.sentiment = sentiment;
    }
}
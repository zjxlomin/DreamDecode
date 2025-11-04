package est.DreamDecode.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "scenes")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Scene {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long sceneId;

    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "emotion", nullable = false)
    private String emotion;

    @Column(name = "interpretation", nullable = false)
    private String interpretation;

    @ManyToOne
    @JoinColumn(name = "dream_id")
    private Dream dream;
}

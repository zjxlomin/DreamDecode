package est.DreamDecode.dto;

import est.DreamDecode.domain.Scene;
import lombok.Getter;

@Getter
public class SceneResponse {
    private String content;
    private String emotion;
    private String interpretation;

    public  SceneResponse(Scene scene) {
        this.content = scene.getContent();
        this.emotion = scene.getEmotion();
        this.interpretation = scene.getInterpretation();
    }
}

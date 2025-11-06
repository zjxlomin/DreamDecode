package est.DreamDecode.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SentimentResult {
  private double score;
  private double magnitude;

  public SentimentResult(double score, double magnitude) {
    this.score = score;
    this.magnitude = magnitude;
  }
}
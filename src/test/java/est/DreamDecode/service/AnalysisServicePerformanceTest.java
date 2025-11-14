package est.DreamDecode.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class AnalysisServicePerformanceTest {

    private static final String SAMPLE_ANALYSIS = """
            {
              "analysis": [
                {
                  "scene": "어두운 숲길을 걷고 있다",
                  "emotion": "불안",
                  "interpretation": "현실의 불확실성에 대한 두려움"
                },
                {
                  "scene": "갑자기 밝은 빛이 비추며 길이 열린다",
                  "emotion": "안도",
                  "interpretation": "문제가 해결될 것이라는 기대감"
                },
                {
                  "scene": "친구와 함께 웃으며 길을 마무리한다",
                  "emotion": "행복",
                  "interpretation": "주변의 지지가 큰 힘이 되고 있음"
                }
              ],
              "insight": "두려움과 기대가 교차하지만 결국 주변의 도움으로 안정을 찾는 꿈입니다.",
              "suggestion": "도움을 주는 사람들에게 감사의 표현을 하고, 자신의 고민을 솔직히 나누어 보세요.",
              "categories": ["불안", "관계", "희망"],
              "tags": ["숲", "빛", "친구", "안정"],
              "summary": "불안한 숲길을 헤치고 나아가 결국 친구와 함께 안정을 찾은 꿈"
            }
            """;

    @Test
    @DisplayName("최적화된 JSON 파싱이 기존 방식보다 빠르게 수행된다")
    void optimizedParsingIsFasterThanLegacy() {
        // warm-up
        measureLegacyParsing(500);
        measureOptimizedParsing(500);

        int iterations = 5_000;
        long legacyTime = measureLegacyParsing(iterations);
        long optimizedTime = measureOptimizedParsing(iterations);

        // 최소 20% 이상 개선되었는지 확인
        System.out.printf("기존 방식: %d ns, 최적화 방식: %d ns%n", legacyTime, optimizedTime);

        assertThat((double) optimizedTime)
                .as("최적화된 파싱 소요 시간(ns)")
                .withFailMessage("""
                        기존 방식: %d ns
                        최적화 방식: %d ns
                        """, legacyTime, optimizedTime)
                .isLessThan(legacyTime * 0.8);
    }

    private long measureLegacyParsing(int iterations) {
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            JSONObject dreamAnalysis = new JSONObject(SAMPLE_ANALYSIS);
            List<Object> scenes = dreamAnalysis.getJSONArray("analysis").toList();
            for (Object s : scenes) {
                ObjectMapper mapper = new ObjectMapper();
                Map<String, String> map = mapper.convertValue(s, Map.class);
                String content = map.get("scene");
                String emotion = map.get("emotion");
                String interpretation = map.get("interpretation");
                consume(content, emotion, interpretation);
            }

            List<Object> categoriesList = dreamAnalysis.getJSONArray("categories").toList();
            categoriesList.stream().map(Object::toString).collect(Collectors.joining(","));
            List<Object> tagsList = dreamAnalysis.getJSONArray("tags").toList();
            tagsList.stream().map(Object::toString).collect(Collectors.joining(","));
            dreamAnalysis.getString("summary");
        }
        return System.nanoTime() - start;
    }

    private long measureOptimizedParsing(int iterations) {
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            JSONObject dreamAnalysis = new JSONObject(SAMPLE_ANALYSIS);
            IntStream.range(0, dreamAnalysis.getJSONArray("analysis").length()).forEach(idx -> {
                var sceneJson = dreamAnalysis.getJSONArray("analysis").getJSONObject(idx);
                String content = sceneJson.getString("scene");
                String emotion = sceneJson.getString("emotion");
                String interpretation = sceneJson.getString("interpretation");
                consume(content, emotion, interpretation);
            });

            var categoriesArray = dreamAnalysis.getJSONArray("categories");
            IntStream.range(0, categoriesArray.length())
                    .mapToObj(categoriesArray::getString)
                    .collect(Collectors.joining(","));

            var tagsArray = dreamAnalysis.getJSONArray("tags");
            IntStream.range(0, tagsArray.length())
                    .mapToObj(tagsArray::getString)
                    .collect(Collectors.joining(","));

            dreamAnalysis.getString("summary");
        }
        return System.nanoTime() - start;
    }

    private static volatile int blackhole;

    private void consume(String... values) {
        for (String value : values) {
            blackhole += value.hashCode();
        }
    }
}


package est.DreamDecode.repository;

import est.DreamDecode.domain.Analysis;
import est.DreamDecode.domain.Dream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestReporter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@DataJpaTest(showSql = false)
@TestPropertySource(properties = {
    "spring.jpa.show-sql=false",
    "spring.jpa.properties.hibernate.format_sql=false",
    "spring.jpa.properties.hibernate.use_sql_comments=false",
    "spring.sql.init.mode=never",
    "logging.level.root=WARN",
    "logging.level.org.hibernate=ERROR",
    "logging.level.org.springframework.orm.jpa=ERROR",
    "logging.level.org.springframework.jdbc=ERROR"
})
class AnalysisLookupPerformanceTest {

    @Autowired
    private AnalysisRepository analysisRepository;

    @Autowired
    private DreamRepository dreamRepository;

    private List<Dream> persistedDreams;

    @BeforeEach
    void setUp() {
        // 성능 차이를 확인하기 위해 충분한 데이터는 유지하되, SQL 로그는 최소화
        int sampleSize = 500;
        List<Dream> dreams = new ArrayList<>(sampleSize);
        for (int i = 0; i < sampleSize; i++) {
            dreams.add(Dream.builder()
                    .userId(1L)
                    .title("title-" + i)
                    .content("content-" + i)
                    .categories(null)
                    .tags(null)
                    .published(false)
                    .build());
        }
        persistedDreams = dreamRepository.saveAll(dreams);

        List<Analysis> analyses = new ArrayList<>(sampleSize);
        for (Dream dream : persistedDreams) {
            Analysis analysis = new Analysis();
            analysis.setInsight("insight");
            analysis.setSuggestion("suggestion");
            analysis.setSummary("summary");
            analysis.setSentiment(0.1d);
            analysis.setMagnitude(0.2d);
            analysis.setDream(dream);
            analyses.add(analysis);
        }
        analysisRepository.saveAll(analyses);
    }

    @Test
    @DisplayName("dreamId 단건 조회 최적화가 기존 전체 조회 대비 빠르게 동작한다")
    void optimizedLookupIsFasterThanLegacy(TestReporter reporter) {
        Long targetDreamId = persistedDreams.get(persistedDreams.size() / 2).getId();

        // warm-up
        legacyLookup(targetDreamId);
        optimizedLookup(targetDreamId);

        // 성능 차이를 확인하기 위한 적절한 반복 횟수
        int iterations = 50;
        long legacyTimeNs = measure(iterations, () -> legacyLookup(targetDreamId));
        long optimizedTimeNs = measure(iterations, () -> optimizedLookup(targetDreamId));

        double legacyTotalMs = toMillis(legacyTimeNs);
        double optimizedTotalMs = toMillis(optimizedTimeNs);
        double legacyAvgUs = toMicrosPerIteration(legacyTimeNs, iterations);
        double optimizedAvgUs = toMicrosPerIteration(optimizedTimeNs, iterations);
        double improvement = ((legacyTimeNs - optimizedTimeNs) / (double) legacyTimeNs) * 100.0;

        // 성능 결과를 포맷팅
        String resultMessage = String.format(
                "========================================%n" +
                "     성능 테스트 결과%n" +
                "========================================%n" +
                "기존 방식 (legacy):%n" +
                "  총 %d회 반복 - %.2fms (평균 %.3fµs/회)%n" +
                "최적화 방식 (optimized):%n" +
                "  총 %d회 반복 - %.2fms (평균 %.3fµs/회)%n" +
                "성능 개선: %.1f%%%n" +
                "========================================",
                iterations, legacyTotalMs, legacyAvgUs,
                iterations, optimizedTotalMs, optimizedAvgUs,
                improvement
        );
        
        // TestReporter를 통해 출력 (IDE 테스트 실행 뷰에서 확인 가능)
        // IntelliJ IDEA: 테스트 실행 후 "Test output" 또는 "Standard output" 탭 확인
        reporter.publishEntry("성능 테스트 결과", resultMessage);
        
        // 표준 출력에도 출력 시도 (일부 IDE에서 표시될 수 있음)
        System.out.println();
        System.out.println(resultMessage);
        System.out.flush();

        // Assertion을 통해 성능 결과가 항상 확인 가능하도록
        // 테스트가 성공해도 성능 정보는 assertion 메시지에 포함
        String assertionMessage = String.format(
                "최적화된 조회 방식이 기존 방식보다 빠르거나 동일해야 합니다.%n%n" +
                "%s%n%n" +
                "※ 성능 결과는 테스트 리포트에서도 확인할 수 있습니다.",
                resultMessage
        );
        
        Assertions.assertThat(optimizedTimeNs)
                .as(assertionMessage)
                .isLessThan(legacyTimeNs);
    }

    private Analysis legacyLookup(Long dreamId) {
        return analysisRepository.findAll()
                .stream()
                .filter(analysis -> analysis.getDream().getId().equals(dreamId))
                .findFirst()
                .orElseThrow();
    }

    private Analysis optimizedLookup(Long dreamId) {
        return analysisRepository.findByDreamId(dreamId)
                .orElseThrow();
    }

    private long measure(int iterations, Supplier<Analysis> supplier) {
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            supplier.get();
        }
        return System.nanoTime() - start;
    }

    private double toMillis(long nanos) {
        return nanos / 1_000_000.0;
    }

    private double toMicrosPerIteration(long nanos, int iterations) {
        return nanos / (double) iterations / 1_000.0;
    }
}


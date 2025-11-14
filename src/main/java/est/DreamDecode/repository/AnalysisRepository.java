package est.DreamDecode.repository;

import est.DreamDecode.domain.Analysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AnalysisRepository extends JpaRepository<Analysis, Long> {
    @Query("select a from Analysis a where a.dream.id = :dreamId")
    Optional<Analysis> findByDreamId(@Param("dreamId") Long dreamId);
}
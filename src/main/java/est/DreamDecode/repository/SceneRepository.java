package est.DreamDecode.repository;

import est.DreamDecode.domain.Scene;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SceneRepository extends JpaRepository<Scene, Long> {
    void deleteByDreamId(Long dreamId);
}

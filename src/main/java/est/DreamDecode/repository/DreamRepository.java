package est.DreamDecode.repository;

import est.DreamDecode.domain.Dream;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DreamRepository extends JpaRepository<Dream, Long> {

  List<Dream> findAllByIsPublicTrue();
}

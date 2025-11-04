package est.DreamDecode.repository;

import est.DreamDecode.domain.Dream;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DreamRepository extends JpaRepository<Dream, Long> {

  List<Dream> findAllByPublishedTrue();
  
  @Query(value = "SELECT * FROM dreams WHERE categories LIKE '%' || :category || '%' AND published = true", nativeQuery = true)
  List<Dream> findByCategoriesContaining(@Param("category") String category);
  
  @Query(value = "SELECT * FROM dreams WHERE tags LIKE '%' || :tag || '%' AND published = true", nativeQuery = true)
  List<Dream> findByTagsContaining(@Param("tag") String tag);
  
  @Query(value = "SELECT * FROM dreams WHERE UPPER(title) LIKE UPPER('%' || :title || '%') AND published = true", nativeQuery = true)
  List<Dream> findByTitleContaining(@Param("title") String title);
}

package est.DreamDecode.repository;

import est.DreamDecode.domain.Dream;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DreamRepository extends JpaRepository<Dream, Long> {

  List<Dream> findAllByPublishedTrue();
  
  // 최신순 정렬 (생성일 기준 내림차순)
  Page<Dream> findByPublishedTrueOrderByCreatedAtDesc(Pageable pageable);
  
  @Query(value = "SELECT * FROM dreams WHERE categories LIKE '%' || :category || '%' AND published = true ORDER BY created_at DESC", nativeQuery = true)
  List<Dream> findByCategoriesContaining(@Param("category") String category);
  
  @Query(value = "SELECT * FROM dreams WHERE categories LIKE '%' || :category || '%' AND published = true ORDER BY created_at DESC", nativeQuery = true)
  Page<Dream> findByCategoriesContaining(@Param("category") String category, Pageable pageable);
  
  @Query(value = "SELECT * FROM dreams WHERE tags LIKE '%' || :tag || '%' AND published = true ORDER BY created_at DESC", nativeQuery = true)
  List<Dream> findByTagsContaining(@Param("tag") String tag);
  
  @Query(value = "SELECT * FROM dreams WHERE tags LIKE '%' || :tag || '%' AND published = true ORDER BY created_at DESC", nativeQuery = true)
  Page<Dream> findByTagsContaining(@Param("tag") String tag, Pageable pageable);
  
  @Query(value = "SELECT * FROM dreams WHERE UPPER(title) LIKE UPPER('%' || :title || '%') AND published = true ORDER BY created_at DESC", nativeQuery = true)
  List<Dream> findByTitleContaining(@Param("title") String title);
  
  @Query(value = "SELECT * FROM dreams WHERE UPPER(title) LIKE UPPER('%' || :title || '%') AND published = true ORDER BY created_at DESC", nativeQuery = true)
  Page<Dream> findByTitleContaining(@Param("title") String title, Pageable pageable);
  
  // 현재 로그인한 사용자의 꿈 조회 (최신순)
  Page<Dream> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}

package sfedu.ictis.woi.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sfedu.ictis.woi.model.entity.SubcategoryEntity;

@Repository
public interface SubcategoryRepository extends JpaRepository<SubcategoryEntity, Integer> {
    @Query("""
            SELECT s FROM SubcategoryEntity s
            WHERE s.category.id = :categoryId
              AND (:search IS NULL OR :search = '' OR LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY s.name ASC
            """)
    Page<SubcategoryEntity> findByCategoryWithSearch(
            @Param("categoryId") Integer categoryId,
            @Param("search") String search,
            Pageable pageable
    );
}
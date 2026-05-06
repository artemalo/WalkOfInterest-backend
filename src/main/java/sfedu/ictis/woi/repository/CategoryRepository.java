package sfedu.ictis.woi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import sfedu.ictis.woi.model.entity.CategoryEntity;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<CategoryEntity, Integer> {
    @Query("""
        SELECT DISTINCT c FROM CategoryEntity c
        LEFT JOIN FETCH c.subcategories s
        ORDER BY c.id
    """)
    List<CategoryEntity> findAllWithSubcategories();
}
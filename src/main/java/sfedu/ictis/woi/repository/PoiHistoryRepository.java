package sfedu.ictis.woi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sfedu.ictis.woi.model.entity.PoiHistoryEntity;

import java.util.List;

@Repository
public interface PoiHistoryRepository extends JpaRepository<PoiHistoryEntity, Long> {
    List<PoiHistoryEntity> findAllByPoiIdOrderByChangedAtDesc(Long poiId);
}

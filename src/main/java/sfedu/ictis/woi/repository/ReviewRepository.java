package sfedu.ictis.woi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sfedu.ictis.woi.model.entity.PoiEntity;
import sfedu.ictis.woi.model.entity.ReviewEntity;
import sfedu.ictis.woi.model.entity.UserEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<ReviewEntity, Long> {
    List<ReviewEntity> findAllByUserOrderByCreatedAtDesc(UserEntity user);

    long countByUser(UserEntity user);

    List<ReviewEntity> findAllByPoiOrderByCreatedAtDesc(PoiEntity poi);

    long countByPoi(PoiEntity poi);

    Optional<ReviewEntity> findByPoiAndUser(PoiEntity poi, UserEntity user);
}
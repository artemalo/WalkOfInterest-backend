package sfedu.ictis.woi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sfedu.ictis.woi.model.entity.PoiEntity;
import sfedu.ictis.woi.model.entity.ReviewEntity;
import sfedu.ictis.woi.model.entity.UserEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<ReviewEntity, Long> {
    @Query("SELECT r FROM ReviewEntity r JOIN FETCH r.poi WHERE r.user = :user ORDER BY r.createdAt DESC")
    List<ReviewEntity> findAllByUserOrderByCreatedAtDesc(@Param("user") UserEntity user);

    @Query("SELECT COUNT(r) FROM ReviewEntity r JOIN r.poi WHERE r.user = :user")
    long countByUser(@Param("user") UserEntity user);

    @Query("SELECT r FROM ReviewEntity r JOIN FETCH r.poi p WHERE p = :poi ORDER BY r.createdAt DESC")
    List<ReviewEntity> findAllByPoiOrderByCreatedAtDesc(@Param("poi") PoiEntity poi);

    @Query("SELECT r FROM ReviewEntity r JOIN FETCH r.poi p WHERE p = :poi AND r.user = :user")
    Optional<ReviewEntity> findByPoiAndUser(@Param("poi") PoiEntity poi, @Param("user") UserEntity user);
}
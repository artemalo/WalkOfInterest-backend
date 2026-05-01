package sfedu.ictis.woi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sfedu.ictis.woi.model.entity.ReviewEntity;
import sfedu.ictis.woi.model.entity.UserEntity;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<ReviewEntity, Long> {
    List<ReviewEntity> findAllByUserOrderByCreatedAtDesc(UserEntity user);

    long countByUser(UserEntity user);
}
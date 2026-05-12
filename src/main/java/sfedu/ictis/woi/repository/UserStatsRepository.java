package sfedu.ictis.woi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import sfedu.ictis.woi.model.entity.UserEntity;
import sfedu.ictis.woi.model.entity.UserStatsEntity;

import java.util.Optional;

@Repository
public interface UserStatsRepository extends JpaRepository<UserStatsEntity, Long> {
    Optional<UserStatsEntity> findByUser(UserEntity user);

    @Query("SELECT u FROM UserStatsEntity u WHERE u.user.id = :userId")
    Optional<UserStatsEntity> findByUserId(Long userId);
}

package sfedu.ictis.woi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sfedu.ictis.woi.model.entity.RefreshTokenEntity;
import sfedu.ictis.woi.model.entity.UserEntity;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, Long> {
    Optional<RefreshTokenEntity> findByToken(String token);
    void deleteByUser(UserEntity user);
}
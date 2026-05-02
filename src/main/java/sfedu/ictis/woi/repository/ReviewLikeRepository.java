package sfedu.ictis.woi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sfedu.ictis.woi.model.entity.ReviewLikeEntity;
import sfedu.ictis.woi.model.entity.id.ReviewLikeId;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewLikeRepository extends JpaRepository<ReviewLikeEntity, ReviewLikeId> {
    @Query("""
        SELECT rl.review.id AS reviewId, rl.value AS value, COUNT(rl) AS cnt
        FROM ReviewLikeEntity rl
        WHERE rl.review.id IN :reviewIds
        GROUP BY rl.review.id, rl.value
    """)
    List<ReviewLikeAggregate> aggregateByReviewIds(@Param("reviewIds") List<Long> reviewIds);

    @Query("""
        SELECT rl FROM ReviewLikeEntity rl
        WHERE rl.user.id = :userId AND rl.review.id IN :reviewIds
    """)
    List<ReviewLikeEntity> findByUserAndReviews(
            @Param("userId") Long userId,
            @Param("reviewIds") List<Long> reviewIds
    );

    @Query("""
        SELECT rl FROM ReviewLikeEntity rl
        WHERE rl.user.id = :userId AND rl.review.id = :reviewId
    """)
    Optional<ReviewLikeEntity> findByUserAndReview(
            @Param("userId") Long userId,
            @Param("reviewId") Long reviewId
    );

    interface ReviewLikeAggregate {
        Long getReviewId();
        Boolean getValue();
        Long getCnt();
    }
}
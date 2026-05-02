package sfedu.ictis.woi.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import sfedu.ictis.woi.exception.InvalidCredentialsException;
import sfedu.ictis.woi.exception.ResourceNotFoundException;
import sfedu.ictis.woi.model.dto.ReactionType;
import sfedu.ictis.woi.model.dto.ReviewReactionResponseDTO;
import sfedu.ictis.woi.model.entity.ReviewEntity;
import sfedu.ictis.woi.model.entity.ReviewLikeEntity;
import sfedu.ictis.woi.model.entity.UserEntity;
import sfedu.ictis.woi.repository.ReviewLikeRepository;
import sfedu.ictis.woi.repository.ReviewRepository;
import sfedu.ictis.woi.repository.UserRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReviewLikeService {
    private final ReviewRepository reviewRepository;
    private final ReviewLikeRepository reviewLikeRepository;
    private final UserRepository userRepository;

    @Transactional
    public ReviewReactionResponseDTO setReaction(
            Long reviewId,
            ReactionType type,
            Authentication authentication
    ) {
        UserEntity user = getAuthenticatedUser(authentication);

        ReviewEntity review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Отзыв не найден: " + reviewId));

        Optional<ReviewLikeEntity> existing =
                reviewLikeRepository.findByUserAndReview(user.getId(), review.getId());

        boolean targetValue = type == ReactionType.LIKE;
        ReactionType resultingReaction;

        if (existing.isEmpty()) {
            ReviewLikeEntity entity = new ReviewLikeEntity();
            entity.setReview(review);
            entity.setUser(user);
            entity.setValue(targetValue);
            reviewLikeRepository.save(entity);
            resultingReaction = type;
        } else {
            ReviewLikeEntity entity = existing.get();
            if (Boolean.valueOf(targetValue).equals(entity.getValue())) {
                reviewLikeRepository.delete(entity);
                resultingReaction = null;
            } else {
                entity.setValue(targetValue);
                reviewLikeRepository.save(entity);
                resultingReaction = type;
            }
        }

        // синхронизация перед агрегацией в той же транзакции
        reviewLikeRepository.flush();

        int[] counters = countLikes(review.getId());

        return ReviewReactionResponseDTO.builder()
                .reviewId(review.getId())
                .likes(counters[0])
                .dislikes(counters[1])
                .myReaction(resultingReaction)
                .build();
    }

    public Map<Long, ReactionType> collectMyReactions(Long userId, List<Long> reviewIds) {
        if (userId == null || reviewIds == null || reviewIds.isEmpty()) {
            return Map.of();
        }

        List<ReviewLikeEntity> mine = reviewLikeRepository.findByUserAndReviews(userId, reviewIds);
        Map<Long, ReactionType> result = new HashMap<>();
        for (ReviewLikeEntity rl : mine) {
            ReactionType type = Boolean.TRUE.equals(rl.getValue())
                    ? ReactionType.LIKE
                    : ReactionType.DISLIKE;
            result.put(rl.getReview().getId(), type);
        }
        return result;
    }

    private int[] countLikes(Long reviewId) {
        var aggregates = reviewLikeRepository.aggregateByReviewIds(List.of(reviewId));
        int likes = 0;
        int dislikes = 0;
        for (var agg : aggregates) {
            if (Boolean.TRUE.equals(agg.getValue())) {
                likes = agg.getCnt().intValue();
            } else if (Boolean.FALSE.equals(agg.getValue())) {
                dislikes = agg.getCnt().intValue();
            }
        }
        return new int[]{likes, dislikes};
    }

    private UserEntity getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName() == null) {
            throw new InvalidCredentialsException();
        }
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
    }
}
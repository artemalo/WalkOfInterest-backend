package sfedu.ictis.woi.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import sfedu.ictis.woi.mapper.UserMapper;
import sfedu.ictis.woi.model.dto.ReactionType;
import sfedu.ictis.woi.model.dto.ReviewDTO;
import sfedu.ictis.woi.model.entity.ReviewEntity;
import sfedu.ictis.woi.model.entity.UserEntity;
import sfedu.ictis.woi.repository.ReviewLikeRepository;
import sfedu.ictis.woi.repository.UserRepository;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewLikeRepository reviewLikeRepository;
    private final ReviewLikeService reviewLikeService;
    private final UserRepository userRepository;

    public List<ReviewDTO> enrichAndMapReviews(List<ReviewEntity> reviews, Authentication authentication, String lang) {
        if (reviews == null || reviews.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, int[]> likesMap = collectLikesMap(reviews);
        Map<Long, ReactionType> myReactions = collectMyReactions(authentication, reviews);

        return reviews.stream()
                .map(r -> {
                    int[] ld = likesMap.getOrDefault(r.getId(), new int[]{0, 0});
                    return UserMapper.mapToReviewDTO(r, ld[0], ld[1], myReactions.get(r.getId()), lang);
                })
                .toList();
    }

    private Map<Long, int[]> collectLikesMap(List<ReviewEntity> reviews) {
        List<Long> reviewIds = reviews.stream().map(ReviewEntity::getId).toList();
        var aggregates = reviewLikeRepository.aggregateByReviewIds(reviewIds);

        Map<Long, int[]> result = new HashMap<>();
        for (var agg : aggregates) {
            int[] ld = result.computeIfAbsent(agg.getReviewId(), _ -> new int[]{0, 0});
            if (Boolean.TRUE.equals(agg.getValue())) {
                ld[0] += agg.getCnt().intValue();
            } else if (Boolean.FALSE.equals(agg.getValue())) {
                ld[1] += agg.getCnt().intValue();
            }
        }
        return result;
    }

    private Map<Long, ReactionType> collectMyReactions(Authentication authentication, List<ReviewEntity> reviews) {
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName() == null) {
            return Map.of();
        }
        Optional<UserEntity> userOpt = userRepository.findByEmail(authentication.getName());
        if (userOpt.isEmpty()) return Map.of();

        List<Long> reviewIds = reviews.stream().map(ReviewEntity::getId).toList();
        return reviewLikeService.collectMyReactions(userOpt.get().getId(), reviewIds);
    }
}
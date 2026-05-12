package sfedu.ictis.woi.mapper;

import sfedu.ictis.woi.model.dto.ReactionType;
import sfedu.ictis.woi.model.dto.ReviewDTO;
import sfedu.ictis.woi.model.dto.UserDTO;
import sfedu.ictis.woi.model.dto.UserProfileDTO;
import sfedu.ictis.woi.model.entity.PoiLanguesEntity;
import sfedu.ictis.woi.model.entity.ReviewEntity;
import sfedu.ictis.woi.model.entity.UserEntity;
import sfedu.ictis.woi.model.entity.UserStatsEntity;

import java.util.List;

public class UserMapper {

    private UserMapper() {}

    public static UserProfileDTO mapToProfileDTO(UserEntity user, long countComments, UserStatsEntity stats) {
        return UserProfileDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .bio(user.getBio())
                .photoUrl(user.getPhotoUrl())
                .countTrips(stats != null ? stats.getCountTrips() : 0)
                .countSpots(stats != null ? stats.getCountSpots() : 0)
                .countComments((int) countComments)
                .build();
    }

    public static ReviewDTO mapToReviewDTO(
            ReviewEntity review,
            int likes,
            int dislikes,
            String targetLang
    ) {
        return mapToReviewDTO(review, likes, dislikes, null, targetLang);
    }

    public static ReviewDTO mapToReviewDTO(
            ReviewEntity review,
            int likes,
            int dislikes,
            ReactionType myReaction,
            String targetLang
    ) {
        UserEntity author = review.getUser();
        return ReviewDTO.builder()
                .id(review.getId())
                .authorUsername(author != null ? author.getUsername() : null)
                .authorAvatarUrl(author != null ? author.getPhotoUrl() : null)
                .poiId(review.getPoi() != null ? review.getPoi().getId() : null)
                .poiName(extractPoiName(review, targetLang))
                .content(review.getText())
                .rating(review.getRate() != null ? review.getRate().intValue() : 0)
                .likes(likes)
                .dislikes(dislikes)
                .myReaction(myReaction)
                .createdAt(review.getCreatedAt())
                .build();
    }

    private static String extractPoiName(ReviewEntity review, String targetLang) {
        if (review.getPoi() == null || review.getPoi().getLocales() == null) return null;

        List<PoiLanguesEntity> locales = review.getPoi().getLocales();
        if (locales.isEmpty()) return null;

        for (PoiLanguesEntity l : locales) {
            if (l.getLangue().equals(targetLang)) return l.getPoiName();
        }
        for (PoiLanguesEntity l : locales) {
            if (l.getLangue().equals("default")) return l.getPoiName();
        }
        for (PoiLanguesEntity l : locales) {
            if (l.getLangue().equals("en")) return l.getPoiName();
        }
        return locales.getFirst().getPoiName();
    }

    public static UserDTO mapToUserDTO(UserEntity user) {
        return new UserDTO(
                user.getId(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getCreatedAt()
        );
    }
}
package sfedu.ictis.woi.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import sfedu.ictis.woi.exception.InvalidCredentialsException;
import sfedu.ictis.woi.exception.ResourceNotFoundException;
import sfedu.ictis.woi.exception.UserAlreadyExistsException;
import sfedu.ictis.woi.mapper.UserMapper;
import sfedu.ictis.woi.model.UpdateProfileRequest;
import sfedu.ictis.woi.model.dto.ReactionType;
import sfedu.ictis.woi.model.dto.ReviewDTO;
import sfedu.ictis.woi.model.dto.UserProfileDTO;
import sfedu.ictis.woi.model.entity.ReviewEntity;
import sfedu.ictis.woi.model.entity.UserEntity;
import sfedu.ictis.woi.repository.ReviewLikeRepository;
import sfedu.ictis.woi.repository.ReviewRepository;
import sfedu.ictis.woi.repository.UserRepository;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewLikeRepository reviewLikeRepository;
    private final ReviewLikeService reviewLikeService;

    public UserProfileDTO getMyProfile(Authentication authentication) {
        UserEntity user = getAuthenticatedUser(authentication);
        long count = reviewRepository.countByUser(user);
        return UserMapper.mapToProfileDTO(user, count);
    }

    public UserProfileDTO getProfileByUsername(String username) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден: " + username));
        long count = reviewRepository.countByUser(user);
        return UserMapper.mapToProfileDTO(user, count);
    }

    public List<ReviewDTO> getReviewsByUsername(String username, Authentication authentication, String lang) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден: " + username));

        List<ReviewEntity> reviews = reviewRepository.findAllByUserOrderByCreatedAtDesc(user);
        if (reviews.isEmpty()) return Collections.emptyList();

        Map<Long, int[]> likesMap = collectLikesMap(reviews);
        Map<Long, ReactionType> myReactions = collectMyReactions(authentication, reviews);

        return reviews.stream()
                .map(r -> {
                    int[] ld = likesMap.getOrDefault(r.getId(), new int[]{0, 0});
                    return UserMapper.mapToReviewDTO(
                            r, ld[0], ld[1], myReactions.get(r.getId()), lang
                    );
                })
                .toList();
    }

    @Transactional
    public UserProfileDTO updateUsername(Authentication authentication, String newUsername) {
        UserEntity user = getAuthenticatedUser(authentication);

        if (newUsername.equalsIgnoreCase(user.getUsername())) {
            return UserMapper.mapToProfileDTO(user, reviewRepository.countByUser(user));
        }

        if (userRepository.existsByUsername(newUsername)) {
            throw new UserAlreadyExistsException("Никнейм уже занят: " + newUsername);
        }

        user.setUsername(newUsername);
        userRepository.save(user);

        long count = reviewRepository.countByUser(user);
        return UserMapper.mapToProfileDTO(user, count);
    }

    @Transactional
    public UserProfileDTO updateProfileInfo(Authentication authentication, UpdateProfileRequest request) {
        UserEntity user = getAuthenticatedUser(authentication);

        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setBio(request.bio());

        userRepository.save(user);

        long count = reviewRepository.countByUser(user);
        return UserMapper.mapToProfileDTO(user, count);
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

    private UserEntity getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName() == null) {
            throw new InvalidCredentialsException();
        }
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
    }
}
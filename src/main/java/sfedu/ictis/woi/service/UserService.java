package sfedu.ictis.woi.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import sfedu.ictis.woi.exception.BusinessException;
import sfedu.ictis.woi.exception.FileStorageException;
import sfedu.ictis.woi.exception.InvalidCredentialsException;
import sfedu.ictis.woi.exception.ResourceNotFoundException;
import sfedu.ictis.woi.exception.UserAlreadyExistsException;
import sfedu.ictis.woi.mapper.UserMapper;
import sfedu.ictis.woi.model.UpdateProfileRequest;
import sfedu.ictis.woi.model.dto.ReviewDTO;
import sfedu.ictis.woi.model.dto.UserProfileDTO;
import sfedu.ictis.woi.model.entity.PoiStatus;
import sfedu.ictis.woi.model.entity.ReviewEntity;
import sfedu.ictis.woi.model.entity.UserEntity;
import sfedu.ictis.woi.model.entity.UserStatsEntity;
import sfedu.ictis.woi.repository.PoiRepository;
import sfedu.ictis.woi.repository.ReviewRepository;
import sfedu.ictis.woi.repository.UserRepository;
import sfedu.ictis.woi.repository.UserStatsRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${app.base-url}")
    private String baseUrl;

    private final UserRepository userRepository;
    private final UserStatsRepository userStatsRepository;
    private final PoiRepository poiRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewService reviewService;

    public UserProfileDTO getMyProfile(Authentication authentication) {
        UserEntity user = getAuthenticatedUser(authentication);
        long countComments = reviewRepository.countByUser(user);
        UserStatsEntity stats = getOrCreateStats(user);
        return UserMapper.mapToProfileDTO(user, countComments, stats);
    }

    public UserProfileDTO getProfileByUsername(String username) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден: " + username));
        long countComments = reviewRepository.countByUser(user);
        UserStatsEntity stats = getOrCreateStats(user);
        return UserMapper.mapToProfileDTO(user, countComments, stats);
    }

    public List<ReviewDTO> getReviewsByUsername(String username, Authentication authentication, String lang) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден: " + username));

        List<ReviewEntity> reviews = reviewRepository.findAllByUserOrderByCreatedAtDesc(user);

        return reviewService.enrichAndMapReviews(reviews, authentication, lang);
    }

    @Transactional
    public UserProfileDTO updateUsername(Authentication authentication, String newUsername) {
        UserEntity user = getAuthenticatedUser(authentication);

        if (newUsername.equalsIgnoreCase(user.getUsername())) {
            UserStatsEntity stats = getOrCreateStats(user);
            return UserMapper.mapToProfileDTO(user, reviewRepository.countByUser(user), stats);
        }

        if (userRepository.existsByUsername(newUsername)) {
            throw new UserAlreadyExistsException("Никнейм уже занят: " + newUsername);
        }

        user.setUsername(newUsername);
        userRepository.save(user);

        long count = reviewRepository.countByUser(user);
        UserStatsEntity stats = getOrCreateStats(user);
        return UserMapper.mapToProfileDTO(user, count, stats);
    }

    @Transactional
    public UserProfileDTO updateProfileInfo(Authentication authentication, UpdateProfileRequest request) {
        UserEntity user = getAuthenticatedUser(authentication);

        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setBio(request.bio());

        userRepository.save(user);

        long count = reviewRepository.countByUser(user);
        UserStatsEntity stats = getOrCreateStats(user);
        return UserMapper.mapToProfileDTO(user, count, stats);
    }

    @Transactional
    public UserProfileDTO uploadPhoto(Authentication authentication, MultipartFile file) {
        UserEntity user = getAuthenticatedUser(authentication);

        String ext = extractExtension(file);

        String filename = user.getId() + "_" + System.currentTimeMillis() + ext;

        try {
            Path avatarsDir = Paths.get(uploadDir, "avatars").toAbsolutePath();
            Files.createDirectories(avatarsDir);

            String oldPhotoUrl = user.getPhotoUrl();
            if (oldPhotoUrl != null && oldPhotoUrl.contains("/avatars/")) {
                String oldFilename = oldPhotoUrl.substring(oldPhotoUrl.lastIndexOf('/') + 1);
                Path oldFile = avatarsDir.resolve(oldFilename);
                Files.deleteIfExists(oldFile);
            }

            file.transferTo(avatarsDir.resolve(filename).toFile());
        } catch (IOException e) {
            throw new FileStorageException("Не удалось сохранить файл: " + e.getMessage());
        }

        user.setPhotoUrl(baseUrl + "/avatars/" + filename);
        userRepository.save(user);

        long count = reviewRepository.countByUser(user);
        UserStatsEntity stats = getOrCreateStats(user);
        return UserMapper.mapToProfileDTO(user, count, stats);
    }

    @Transactional
    public void incrementTrips(Authentication authentication) {
        UserEntity user = getAuthenticatedUser(authentication);
        UserStatsEntity stats = getOrCreateStats(user);

        stats.setCountTrips(stats.getCountTrips() + 1);
        userStatsRepository.save(stats);
    }

    @Transactional
    public void incrementSpots(Authentication authentication) {
        UserEntity user = getAuthenticatedUser(authentication);
        UserStatsEntity stats = getOrCreateStats(user);

        stats.setCountSpots(stats.getCountSpots() + 1);
        userStatsRepository.save(stats);
    }



    private @NonNull String extractExtension(MultipartFile file) {
        if (file.isEmpty()) throw new BusinessException("Файл не выбран");

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException("Допускаются только изображения");
        }

        String originalFilename = file.getOriginalFilename();
        return (originalFilename != null && originalFilename.contains("."))
                ? originalFilename.substring(originalFilename.lastIndexOf('.'))
                : ".jpg";
    }

    private UserEntity getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName() == null) {
            throw new InvalidCredentialsException();
        }
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
    }

    private UserStatsEntity getOrCreateStats(UserEntity user) {
        return userStatsRepository.findByUser(user)
                .orElseGet(() -> {
                    long countSpots = poiRepository.countByUserAndStatus(user, PoiStatus.APPROVED);
                    UserStatsEntity newStats = UserStatsEntity.builder()
                            .user(user)
                            .countTrips(0)
                            .countSpots((int) countSpots)
                            .build();
                    return userStatsRepository.save(newStats);
                });
    }
}
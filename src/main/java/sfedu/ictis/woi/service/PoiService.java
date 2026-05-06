package sfedu.ictis.woi.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import sfedu.ictis.woi.exception.AccessDeniedException;
import sfedu.ictis.woi.exception.InvalidCredentialsException;
import sfedu.ictis.woi.exception.PoiAlreadyExistsException;
import sfedu.ictis.woi.exception.RateLimitExceededException;
import sfedu.ictis.woi.exception.ResourceNotFoundException;
import sfedu.ictis.woi.mapper.PoiMapper;
import sfedu.ictis.woi.mapper.UserMapper;
import sfedu.ictis.woi.model.dto.*;
import sfedu.ictis.woi.model.entity.*;
import sfedu.ictis.woi.projection.PoiNearbyProjection;
import sfedu.ictis.woi.repository.*;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PoiService {
    // Радиус для /check - 50м, чтобы показать "похожие места рядом".
    // Не путать с existsNearby (5м) - это защита от точечных дублей в createPoi.
    private static final double CHECK_RADIUS_METERS_DEFAULT = 50.0;
    private static final int CHECK_LIMIT = 10;

    private final PoiRepository poiRepository;
    private final UserRepository userRepository;
    private final SubcategoryRepository subcategoryRepository;

    private final ReviewRepository reviewRepository;
    private final ReviewLikeRepository reviewLikeRepository;
    private final ReviewLikeService reviewLikeService;

    private final RateLimiterService rateLimiterService;

    @Value("${app.poi.check.radius-meters:50}")
    private double checkRadiusMeters;

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);


    public PoiInfoDTO getPoiById(Long id, String lang) {
        PoiEntity entity = poiRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("POI не найден: " + id));
        return PoiMapper.mapToInfoDTO(entity, lang);
    }

    public List<PoiCardDTO> getUserPois(Authentication authentication, String lang) {
        UserEntity user = getAuthenticatedUser(authentication);
        List<PoiEntity> userPois = poiRepository.findAllByUser(user);

        return userPois.stream()
                .map(entity -> PoiMapper.mapToCardDTO(entity, lang))
                .collect(Collectors.toList());
    }

    public PoiNearbyCheckResponseDTO checkNearby(PointDTO point, String lang, Authentication authentication) {
        getAuthenticatedUser(authentication);

        if (point == null || point.getLat() == null || point.getLon() == null) {
            throw new IllegalArgumentException("Координаты обязательны");
        }

        double radius = checkRadiusMeters > 0 ? checkRadiusMeters : CHECK_RADIUS_METERS_DEFAULT;

        List<PoiNearbyProjection> projections = poiRepository.findNearbyPois(
                point.getLon(),
                point.getLat(),
                radius,
                lang != null ? lang : "ru",
                CHECK_LIMIT
        );

        List<PoiNearbyDTO> pois = projections.stream()
                .map(PoiMapper::mapNearbyToDTO)
                .toList();

        return new PoiNearbyCheckResponseDTO(!pois.isEmpty(), pois);
    }

    @Transactional
    public PoiInfoDTO createPoi(PoiAddDTO dto, Authentication authentication) {
        UserEntity user = getAuthenticatedUser(authentication);

        if (!rateLimiterService.tryConsumePoiCreate(user.getId())) {
            long retryAfter = rateLimiterService.getPoiCreateRetryAfterSeconds(user.getId());
            throw new RateLimitExceededException(
                    "Вы добавили слишком много мест сегодня. Попробуйте позже.",
                    retryAfter
            );
        }

        if (dto.getPoint() == null || dto.getPoint().getLat() == null || dto.getPoint().getLon() == null) {
            throw new IllegalArgumentException("Координаты обязательны");
        }

        boolean force = Boolean.TRUE.equals(dto.getForce());
        if (!force && poiRepository.existsNearby(dto.getPoint().getLon(), dto.getPoint().getLat())) {
            throw new PoiAlreadyExistsException("Точка уже существует в этой локации или слишком близко");
        }

        PoiEntity entity = new PoiEntity();
        Point point = geometryFactory.createPoint(new Coordinate(dto.getPoint().getLon(), dto.getPoint().getLat()));
        entity.setGeom(point);
        entity.setUser(user);
        entity.setStatus(PoiStatus.PENDING);
        entity.setRejectionReason(null);

        updateSubcategories(entity, dto.getSubcategoriesId());
        addOrUpdateLocale(entity, dto.getLang(), dto.getName(), dto.getDescription());

        PoiEntity savedEntity = poiRepository.save(entity);
        return PoiMapper.mapToInfoDTO(savedEntity, dto.getLang());
    }

    @Transactional
    public PoiInfoDTO updatePoi(Long id, PoiAddDTO dto, Authentication authentication) {
        UserEntity user = getAuthenticatedUser(authentication);
        PoiEntity entity = poiRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("POI не найден: " + id));

        if (user.getRole() != UserRole.ADMIN) {
            if (!entity.getUser().getId().equals(user.getId())) {
                throw new AccessDeniedException("Вы не можете редактировать чужую точку");
            }
        }

        if (dto.getPoint() != null && dto.getPoint().getLat() != null && dto.getPoint().getLon() != null) {
            Point point = geometryFactory.createPoint(new Coordinate(dto.getPoint().getLon(), dto.getPoint().getLat()));
            entity.setGeom(point);
        }

        updateSubcategories(entity, dto.getSubcategoriesId());
        addOrUpdateLocale(entity, dto.getLang(), dto.getName(), dto.getDescription());

        entity.setStatus(PoiStatus.PENDING);
        entity.setRejectionReason(null);

        PoiEntity savedEntity = poiRepository.save(entity);
        return PoiMapper.mapToInfoDTO(savedEntity, dto.getLang());
    }

    public List<PoiAdminDTO> getPoisByStatus(PoiStatus status, Pageable pageable, String targetLang) {
        Page<PoiEntity> poiPage = poiRepository.findAllByStatusIncludingDeleted(status, pageable);
        return poiPage.getContent().stream()
                .map(entity -> PoiMapper.mapToAdminDTO(entity, targetLang))
                .collect(Collectors.toList());
    }

    @Transactional
    public void updatePoiStatus(Long id, PoiStatus status, String rejectionReason) {
        PoiEntity entity = poiRepository.findByIdIncludingDeleted(id)
                .orElseThrow(() -> new ResourceNotFoundException("POI не найден: " + id));

        entity.setStatus(status);
        if (status == PoiStatus.REJECTED) {
            entity.setRejectionReason(
                    rejectionReason != null && !rejectionReason.isBlank()
                            ? rejectionReason
                            : "Причина не указана"
            );
        } else {
            entity.setRejectionReason(null);
        }
        poiRepository.save(entity);

        log.warn("[{}] poi_id: {}", status, entity.getId());
    }

    @Transactional
    public ReviewDTO upsertMyReview(Long poiId, ReviewRequestDTO dto, Authentication authentication, String lang) {
        UserEntity user = getAuthenticatedUser(authentication);

        PoiEntity poi = poiRepository.findById(poiId)
                .orElseThrow(() -> new ResourceNotFoundException("POI не найден: " + poiId));

        ReviewEntity review = reviewRepository.findByPoiAndUser(poi, user)
                .orElseGet(() -> {
                    ReviewEntity r = new ReviewEntity();
                    r.setPoi(poi);
                    r.setUser(user);
                    return r;
                });

        review.setRate(dto.getRating().shortValue());
        review.setText(dto.getContent());

        ReviewEntity saved = reviewRepository.save(review);

        int likes = 0;
        int dislikes = 0;
        var aggregates = reviewLikeRepository.aggregateByReviewIds(List.of(saved.getId()));
        for (var agg : aggregates) {
            if (Boolean.TRUE.equals(agg.getValue())) {
                likes = agg.getCnt().intValue();
            } else if (Boolean.FALSE.equals(agg.getValue())) {
                dislikes = agg.getCnt().intValue();
            }
        }

        ReactionType myReaction = reviewLikeService
                .collectMyReactions(user.getId(), List.of(saved.getId()))
                .get(saved.getId());

        return UserMapper.mapToReviewDTO(saved, likes, dislikes, myReaction, lang);
    }

    public List<ReviewDTO> getReviewsByPoiId(Long poiId, Authentication authentication, String lang) {
        PoiEntity poi = poiRepository.findById(poiId)
                .orElseThrow(() -> new ResourceNotFoundException("POI не найден: " + poiId));

        List<ReviewEntity> reviews = reviewRepository.findAllByPoiOrderByCreatedAtDesc(poi);
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

    private void updateSubcategories(PoiEntity entity, List<Integer> subcategoryIds) {
        if (subcategoryIds != null && !subcategoryIds.isEmpty()) {
            Set<SubcategoryEntity> subcategories = subcategoryIds.stream()
                    .map(id -> subcategoryRepository.findById(id)
                            .orElseThrow(() -> new ResourceNotFoundException("Подкатегория не найдена: " + id)))
                    .collect(Collectors.toSet());
            entity.setSubcategories(subcategories);
        }
    }

    private void addOrUpdateLocale(PoiEntity entity, String lang, String name, String description) {
        if (name == null && description == null) return;

        String targetLang = lang != null ? lang : "default";

        PoiLanguesEntity locale = entity.getLocales().stream()
                .filter(l -> l.getLangue().equals(targetLang))
                .findFirst()
                .orElseGet(() -> {
                    PoiLanguesEntity newLocale = new PoiLanguesEntity();
                    newLocale.setPoi(entity);
                    newLocale.setLangue(targetLang);
                    entity.getLocales().add(newLocale);
                    return newLocale;
                });

        if (name != null) locale.setPoiName(name);
        if (description != null) locale.setPoiDescription(description);
    }
}
package sfedu.ictis.woi.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import sfedu.ictis.woi.exception.*;
import sfedu.ictis.woi.mapper.PoiMapper;
import sfedu.ictis.woi.mapper.UserMapper;
import sfedu.ictis.woi.model.dto.*;
import sfedu.ictis.woi.model.entity.*;
import sfedu.ictis.woi.projection.PoiNearbyProjection;
import sfedu.ictis.woi.repository.*;
import sfedu.ictis.woi.util.HaversineMeters;
import sfedu.ictis.woi.util.ImageCompressionUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    private final PoiHistoryRepository poiHistoryRepository;

    private final ReviewRepository reviewRepository;
    private final ReviewLikeRepository reviewLikeRepository;
    private final ReviewLikeService reviewLikeService;
    private final ReviewService reviewService;

    private final RateLimiterService rateLimiterService;

    @Value("${app.poi.check.radius-meters:50}")
    private double checkRadiusMeters;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${app.base-url}")
    private String baseUrl;

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

        recordHistory(savedEntity.getId(), PoiHistoryAction.CREATED,
                null, PoiStatus.PENDING,
                user.getEmail(), null);

        return PoiMapper.mapToInfoDTO(savedEntity, dto.getLang());
    }

    @Transactional
    public PoiInfoDTO updatePoi(Long id, PoiAddDTO dto, Authentication authentication) {
        UserEntity user = getAuthenticatedUser(authentication);
        PoiEntity entity = poiRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("POI не найден: " + id));

        if (isNotAdmin(user)) {
            UserEntity owner = entity.getUser();
            if (owner != null && !owner.getId().equals(user.getId())) {
                throw new AccessDeniedException("Вы не можете редактировать чужую точку");
            }
        }

        PoiStatus oldStatus = entity.getStatus();

        if (dto.getPoint() != null && dto.getPoint().getLat() != null && dto.getPoint().getLon() != null) {
            Point point = geometryFactory.createPoint(new Coordinate(dto.getPoint().getLon(), dto.getPoint().getLat()));
            entity.setGeom(point);
        }

        updateSubcategories(entity, dto.getSubcategoriesId());
        addOrUpdateLocale(entity, dto.getLang(), dto.getName(), dto.getDescription());

        entity.setStatus(PoiStatus.PENDING);
        entity.setRejectionReason(null);

        PoiEntity savedEntity = poiRepository.save(entity);

        recordHistory(savedEntity.getId(), PoiHistoryAction.UPDATED,
                oldStatus, PoiStatus.PENDING,
                user.getEmail(), null);

        return PoiMapper.mapToInfoDTO(savedEntity, dto.getLang());
    }

    @Transactional
    public PoiInfoDTO supplementPoi(Long id, PoiAddDTO dto, Authentication authentication) {
        UserEntity user = getAuthenticatedUser(authentication);
        PoiEntity entity = poiRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("POI не найден: " + id));

        PoiStatus oldStatus = entity.getStatus();

        if (isNotAdmin(user)) {
            UserEntity owner = entity.getUser();
            if (owner != null && !owner.getId().equals(user.getId())) {
                throw new AccessDeniedException("Вы не можете редактировать чужую точку");
            }
        }

        addSubcategories(entity, dto.getSubcategoriesId());
        fillEmptyLocale(entity, dto.getLang(), dto.getName(), dto.getDescription());

        entity.setStatus(PoiStatus.PENDING);
        entity.setRejectionReason(null);

        PoiEntity savedEntity = poiRepository.save(entity);

        recordHistory(savedEntity.getId(), PoiHistoryAction.SUPPLEMENTED,
                oldStatus, PoiStatus.PENDING,
                user.getEmail(), null);

        return PoiMapper.mapToInfoDTO(savedEntity, dto.getLang());
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

        return reviewService.enrichAndMapReviews(reviews, authentication, lang);
    }

    @Transactional
    public PoiInfoDTO uploadPoiPhoto(Long id, MultipartFile file, Authentication authentication, String lang) {
        UserEntity user = getAuthenticatedUser(authentication);
        PoiEntity poi = poiRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("POI не найден: " + id));

        boolean hasPhoto = poi.getPhoto() != null;

        if (hasPhoto && isNotAdmin(user)) {
            throw new AccessDeniedException("Заменить существующее фото места может только администратор");
        }

        if (file == null || file.isEmpty()) {
            throw new BusinessException("Файл не выбран");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException("Допускаются только изображения");
        }

        String filename = "poi_" + id + "_" + System.currentTimeMillis() + ".jpg";

        try {
            Path poisDir = Paths.get(uploadDir, "pois").toAbsolutePath();
            Files.createDirectories(poisDir);

            PoiPhotoEntity photoEntity = poi.getPhoto();

            if (photoEntity != null && photoEntity.getPhotoUrl() != null) {
                String oldFilename = photoEntity.getPhotoUrl()
                        .substring(photoEntity.getPhotoUrl().lastIndexOf('/') + 1);
                Files.deleteIfExists(poisDir.resolve(oldFilename));
            } else {
                photoEntity = new PoiPhotoEntity();
                photoEntity.setPoi(poi);
            }

            ImageCompressionUtils.compressAndSaveAsJpg(file, poisDir.resolve(filename), 0.3f);

            photoEntity.setPhotoUrl(baseUrl + "/pois/" + filename);
            poi.setPhoto(photoEntity);

            poiRepository.save(poi);

        } catch (IOException e) {
            throw new FileStorageException("Не удалось сохранить фото POI: " + e.getMessage());
        }

        return PoiMapper.mapToInfoDTO(poi, lang);
    }


    public List<PoiAdminDTO> getPoisByStatus(
            PoiStatus status, Pageable pageable, String targetLang,
            String search, String ownerType,
            Double lat, Double lon
    ) {
        String normalizedSearch = (search != null && !search.isBlank()) ? search.trim() : null;
        Long searchId = null;
        if (normalizedSearch != null) {
            try {
                searchId = Long.parseLong(normalizedSearch);
            } catch (NumberFormatException ignored) { }
        }

        String normalizedOwner = (ownerType != null && !ownerType.isBlank()) ? ownerType.trim() : null;

        Page<PoiEntity> poiPage = poiRepository.findAllByStatusWithFilters(
                status.name(), normalizedSearch, searchId, normalizedOwner, lat, lon, pageable
        );

        return poiPage.getContent().stream()
                .map(entity -> PoiMapper.mapToAdminDTO(entity, targetLang))
                .collect(Collectors.toList());
    }

    @Transactional
    public PoiAdminDTO adjustPoiLocation(Long id, PointDTO newPoint) {
        if (newPoint == null || newPoint.getLat() == null || newPoint.getLon() == null) {
            throw new BusinessException("Координаты не указаны");
        }

        PoiEntity entity = poiRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("POI не найден: " + id));

        if (entity.getStatus() != PoiStatus.PENDING) {
            throw new BusinessException("Корректировка координат доступна только для PENDING точек");
        }

        if (entity.getGeom() != null) {
            double origLat = entity.getGeom().getCentroid().getY();
            double origLon = entity.getGeom().getCentroid().getX();

            double distanceMeters = HaversineMeters.haversineMeters(origLat, origLon, newPoint.getLat(), newPoint.getLon());
            if (distanceMeters > 15.0) {
                throw new BusinessException(
                        String.format("Смещение слишком большое: %.1f м (максимум 15 м)", distanceMeters)
                );
            }
        }

        Point point = geometryFactory.createPoint(new Coordinate(newPoint.getLon(), newPoint.getLat()));
        entity.setGeom(point);
        poiRepository.save(entity);

        return PoiMapper.mapToAdminDTO(entity, "default");
    }

    @Transactional
    public void updatePoiStatus(Long id, PoiStatus status, String rejectionReason) {
        PoiEntity entity = poiRepository.findByIdIncludingDeleted(id)
                .orElseThrow(() -> new ResourceNotFoundException("POI не найден: " + id));

        PoiStatus oldStatus = entity.getStatus();

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

        String actor = currentActorName();
        String note = (status == PoiStatus.REJECTED && entity.getRejectionReason() != null)
                ? entity.getRejectionReason()
                : null;

        recordHistory(entity.getId(), PoiHistoryAction.STATUS_CHANGED,
                oldStatus, status, actor, note);

        log.warn("[{}] poi_id: {}, actor: {}", status, entity.getId(), actor);
    }

    @Transactional
    public void deletePoi(Long id) {
        PoiEntity entity = poiRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("POI не найден: " + id));

        if (entity.getStatus() != PoiStatus.REJECTED) {
            throw new BusinessException("Удалить можно только отклонённую точку (статус REJECTED)");
        }

        String actor = currentActorName();
        recordHistory(entity.getId(), PoiHistoryAction.DELETED,
                entity.getStatus(), null, actor, null);

        poiRepository.delete(entity);
        log.warn("[DELETED] poi_id: {}, actor: {}", id, actor);
    }

    @Transactional
    public void deletePoiPhoto(Long id) {
        PoiEntity poi = poiRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("POI не найден: " + id));

        PoiPhotoEntity photo = poi.getPhoto();
        if (photo == null) return;

        if (photo.getPhotoUrl() != null) {
            try {
                String filename = photo.getPhotoUrl()
                        .substring(photo.getPhotoUrl().lastIndexOf('/') + 1);
                Path poisDir = Paths.get(uploadDir, "pois").toAbsolutePath();
                Files.deleteIfExists(poisDir.resolve(filename));
            } catch (IOException e) {
                log.warn("Не удалось удалить файл фото POI {}: {}", id, e.getMessage());
            }
        }

        poi.setPhoto(null);
        poiRepository.save(poi);
    }

    public List<PoiHistoryDTO> getPoiHistory(Long id) {
        return poiHistoryRepository.findAllByPoiIdOrderByChangedAtDesc(id)
                .stream()
                .map(this::mapHistory)
                .collect(Collectors.toList());
    }



    private boolean isNotAdmin(UserEntity user) {
        return user.getRole() != UserRole.ADMIN;
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

    private void addSubcategories(PoiEntity entity, List<Integer> subcategoryIds) {
        if (subcategoryIds == null || subcategoryIds.isEmpty()) return;

        Set<Integer> existingIds = entity.getSubcategories().stream()
                .map(SubcategoryEntity::getId)
                .collect(Collectors.toSet());

        for (Integer subId : subcategoryIds) {
            if (existingIds.contains(subId)) continue;
            SubcategoryEntity sub = subcategoryRepository.findById(subId)
                    .orElseThrow(() -> new ResourceNotFoundException("Подкатегория не найдена: " + subId));
            entity.getSubcategories().add(sub);
        }
    }

    private PoiLanguesEntity getOrCreateLocale(PoiEntity entity, String lang) {
        String targetLang = lang != null ? lang : "default";

        return entity.getLocales().stream()
                .filter(l -> l.getLangue().equals(targetLang))
                .findFirst()
                .orElseGet(() -> {
                    PoiLanguesEntity newLocale = new PoiLanguesEntity();
                    newLocale.setPoi(entity);
                    newLocale.setLangue(targetLang);
                    entity.getLocales().add(newLocale);
                    return newLocale;
                });
    }

    private void addOrUpdateLocale(PoiEntity entity, String lang, String name, String description) {
        if (name == null && description == null) return;

        PoiLanguesEntity locale = getOrCreateLocale(entity, lang);

        if (name != null) locale.setPoiName(name);
        if (description != null) locale.setPoiDescription(description);
    }

    private void fillEmptyLocale(PoiEntity entity, String lang, String name, String description) {
        boolean hasName = name != null && !name.isBlank();
        boolean hasDescription = description != null && !description.isBlank();
        if (!hasName && !hasDescription) return;

        PoiLanguesEntity locale = getOrCreateLocale(entity, lang);

        if (hasName && (locale.getPoiName() == null || locale.getPoiName().isBlank())) {
            locale.setPoiName(name);
        }
        if (hasDescription && (locale.getPoiDescription() == null || locale.getPoiDescription().isBlank())) {
            locale.setPoiDescription(description);
        }
    }

    private void recordHistory(Long poiId, PoiHistoryAction action,
                               PoiStatus oldStatus, PoiStatus newStatus,
                               String actorUsername, String note) {
        PoiHistoryEntity h = new PoiHistoryEntity();
        h.setPoiId(poiId);
        h.setActionType(action);
        h.setOldStatus(oldStatus);
        h.setNewStatus(newStatus != null ? newStatus : oldStatus);
        h.setActorUsername(actorUsername);
        h.setNote(note);
        poiHistoryRepository.save(h);
    }

    private String currentActorName() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getName() != null) {
            return auth.getName();
        }
        return "system";
    }

    private PoiHistoryDTO mapHistory(PoiHistoryEntity entity) {
        return new PoiHistoryDTO(
                entity.getId(),
                entity.getPoiId(),
                entity.getActorUsername(),
                entity.getActionType(),
                entity.getOldStatus(),
                entity.getNewStatus(),
                entity.getChangedAt(),
                entity.getNote()
        );
    }
}
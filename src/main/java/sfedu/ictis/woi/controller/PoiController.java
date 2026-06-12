package sfedu.ictis.woi.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import sfedu.ictis.woi.api.PoiControllerApi;
import sfedu.ictis.woi.model.dto.*;
import sfedu.ictis.woi.service.PoiService;
import sfedu.ictis.woi.service.RateLimitType;
import sfedu.ictis.woi.service.RateLimiterService;
import sfedu.ictis.woi.service.UserService;
import sfedu.ictis.woi.util.RateLimitKey;

import java.util.List;

@RestController
@RequestMapping("/api/pois")
@RequiredArgsConstructor
public class PoiController implements PoiControllerApi {
    private final PoiService poiService;
    private final UserService userService;
    private final RateLimiterService rateLimiterService;

    @GetMapping("/{id}")
    public PoiInfoDTO getPoiById(
            @PathVariable Long id,
            @RequestParam(defaultValue = "ru") String lang
    ) {
        return poiService.getPoiById(id, lang);
    }

    @GetMapping("/{id}/reviews")
    public List<ReviewDTO> getReviewsByPoiId(
            @PathVariable Long id,
            @RequestParam(defaultValue = "ru") String lang
    ) {
        return poiService.getReviewsByPoiId(
                id,
                SecurityContextHolder.getContext().getAuthentication(),
                lang
        );
    }

    @PutMapping("/{id}/reviews/me")
    public ReviewDTO upsertMyReview(
            @PathVariable Long id,
            @Valid @RequestBody ReviewRequestDTO request,
            @RequestParam(defaultValue = "ru") String lang
    ) {
        rateLimiterService.consumeOrThrow(RateLimitType.REVIEW_UPSERT, RateLimitKey.currentUser(),
                "Слишком много отзывов за час. Попробуйте позже.");
        return poiService.upsertMyReview(
                id,
                request,
                SecurityContextHolder.getContext().getAuthentication(),
                lang
        );
    }

    @PutMapping("/{id}")
    public PoiInfoDTO updatePoi(
            @PathVariable Long id,
            @RequestBody PoiAddDTO poi
    ) {
        rateLimiterService.consumeOrThrow(RateLimitType.POI_UPDATE, RateLimitKey.currentUser(),
                "Слишком много правок мест за час. Попробуйте позже.");
        return poiService.updatePoi(id, poi, SecurityContextHolder.getContext().getAuthentication());
    }

    @GetMapping("/my")
    public List<PoiCardDTO> getUserPois(
            @RequestParam(defaultValue = "ru") String lang
    ) {
        return poiService.getUserPois(SecurityContextHolder.getContext().getAuthentication(), lang);
    }

    @PostMapping
    public PoiInfoDTO createPoi(
            @RequestBody PoiAddDTO poi
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        PoiInfoDTO poiInfoDTO = poiService.createPoi(poi, authentication);
        userService.incrementSpots(authentication);

        return poiInfoDTO;
    }

    @PostMapping("/check")
    public PoiNearbyCheckResponseDTO checkNearby(
            @Valid @RequestBody PoiNearbyCheckRequestDTO request,
            @RequestParam(defaultValue = "ru") String lang
    ) {
        rateLimiterService.consumeOrThrow(RateLimitType.POI_CHECK_NEARBY, RateLimitKey.currentUser(),
                "Слишком много проверок мест за час. Попробуйте позже.");
        return poiService.checkNearby(
                request.getPoint(),
                lang,
                SecurityContextHolder.getContext().getAuthentication()
        );
    }

    @PostMapping("/{id}/supplement")
    public PoiInfoDTO supplementPoi(
            @PathVariable Long id,
            @RequestBody PoiAddDTO poi
    ) {
        rateLimiterService.consumeOrThrow(RateLimitType.POI_SUPPLEMENT, RateLimitKey.currentUser(),
                "Слишком много правок мест за час. Попробуйте позже.");
        return poiService.supplementPoi(
                id,
                poi,
                SecurityContextHolder.getContext().getAuthentication()
        );
    }

    @PostMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PoiInfoDTO> uploadPoiPhoto(
            @PathVariable Long id,
            @RequestParam("photo") MultipartFile photo,
            @RequestParam(defaultValue = "ru") String lang
    ) {
        rateLimiterService.consumeOrThrow(RateLimitType.POI_PHOTO_UPLOAD, RateLimitKey.currentUser(),
                "Превышен дневной лимит загрузки фото. Попробуйте завтра.");
        return ResponseEntity.ok(poiService.uploadPoiPhoto(
                id,
                photo,
                SecurityContextHolder.getContext().getAuthentication(),
                lang
        ));
    }
}
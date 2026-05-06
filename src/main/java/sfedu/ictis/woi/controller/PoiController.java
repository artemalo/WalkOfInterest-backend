package sfedu.ictis.woi.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import sfedu.ictis.woi.api.PoiControllerApi;
import sfedu.ictis.woi.model.dto.*;
import sfedu.ictis.woi.service.PoiService;

import java.util.List;

@RestController
@RequestMapping("/api/pois")
@RequiredArgsConstructor
public class PoiController implements PoiControllerApi {
    private final PoiService poiService;

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
        return poiService.updatePoi(id, poi, SecurityContextHolder.getContext().getAuthentication());
    }

    @GetMapping("/my")
    public List<PoiCardDTO> getUserPois(
            @RequestParam(defaultValue = "ru") String lang
    ) {
        return poiService.getUserPois(SecurityContextHolder.getContext().getAuthentication(), lang);
    }

    @PostMapping
    public PoiInfoDTO createPoi(@RequestBody PoiAddDTO poi) {
        return poiService.createPoi(
                poi,
                SecurityContextHolder.getContext().getAuthentication()
        );
    }

    @PostMapping("/check")
    public PoiNearbyCheckResponseDTO checkNearby(
            @Valid @RequestBody PoiNearbyCheckRequestDTO request,
            @RequestParam(defaultValue = "ru") String lang
    ) {
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
        return poiService.supplementPoi(
                id,
                poi,
                SecurityContextHolder.getContext().getAuthentication()
        );
    }
}
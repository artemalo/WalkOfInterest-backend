package sfedu.ictis.woi.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import sfedu.ictis.woi.api.PoiControllerApi;
import sfedu.ictis.woi.model.dto.PoiAddDTO;
import sfedu.ictis.woi.model.dto.PoiCardDTO;
import sfedu.ictis.woi.model.dto.PoiInfoDTO;
import sfedu.ictis.woi.service.PoiService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/pois")
public class PoiController implements PoiControllerApi {
    private final PoiService poiService;

    @PostMapping
    public PoiInfoDTO createPoi(
            @RequestBody PoiAddDTO poi
    ) {
        return poiService.createPoi(poi, SecurityContextHolder.getContext().getAuthentication());
    }

    @GetMapping("/{id}")
    public PoiInfoDTO getPoiById(
            @PathVariable Long id,
            @RequestParam(defaultValue = "ru") String lang
    ) {
        return poiService.getPoiById(id, lang);
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
}
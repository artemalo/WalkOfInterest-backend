package sfedu.ictis.woi.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import sfedu.ictis.woi.api.PoiControllerApi;
import sfedu.ictis.woi.model.dto.PoiDTO;
import sfedu.ictis.woi.service.PoiService;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PoiController implements PoiControllerApi {
    private final PoiService poiService;

    @PostMapping
    public PoiDTO createPoi(@RequestBody PoiDTO poi) {
        return poiService.createPoi(poi, SecurityContextHolder.getContext().getAuthentication());
    }

    @Override
    public PoiDTO getPoiById(Long id) {
        return null; // TODO: implement
    }

    @Override
    public PoiDTO updatePoi(Long id, PoiDTO poi) {
        return null; // TODO: implement
    }

    @Override
    public List<PoiDTO> getUserPois() {
        return null; // TODO: implement
    }
}
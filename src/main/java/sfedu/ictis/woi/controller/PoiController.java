package sfedu.ictis.woi.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import sfedu.ictis.woi.api.PoiControllerApi;
import sfedu.ictis.woi.model.dto.PoiAddDTO;
import sfedu.ictis.woi.model.dto.PoiCardDTO;
import sfedu.ictis.woi.model.dto.PoiInfoDTO;
import sfedu.ictis.woi.service.PoiService;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PoiController implements PoiControllerApi {
    private final PoiService poiService;

    @PostMapping
    public PoiInfoDTO createPoi(@RequestBody PoiAddDTO poi) {
        return poiService.createPoi(poi, SecurityContextHolder.getContext().getAuthentication());
    }

    @Override
    public PoiInfoDTO getPoiById(Long id) {
        return null; // TODO: implement
    }

    @Override
    public PoiInfoDTO updatePoi(Long id, PoiAddDTO poi) {
        return null; // TODO: implement
    }

    @Override
    public List<PoiCardDTO> getUserPois() {
        return null; // TODO: implement
    }
}
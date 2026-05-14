package sfedu.ictis.woi.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sfedu.ictis.woi.api.AdminControllerApi;
import sfedu.ictis.woi.model.dto.PoiAdminDTO;
import sfedu.ictis.woi.model.dto.PoiHistoryDTO;
import sfedu.ictis.woi.model.dto.PointDTO;
import sfedu.ictis.woi.model.entity.PoiStatus;
import sfedu.ictis.woi.service.PoiService;

import java.util.List;

@RestController
@RequestMapping("/api/admin/pois")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController implements AdminControllerApi {
    private final PoiService poiService;

    @GetMapping
    public List<PoiAdminDTO> getPoisByStatus(
            @RequestParam PoiStatus request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "default") String lang,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String ownerType,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lon
    ) {
        return poiService.getPoisByStatus(
                request, PageRequest.of(page, size), lang,
                search, ownerType, lat, lon
        );
    }

    @PatchMapping("/{id}/status")
    public void updatePoiStatus(
            @PathVariable Long id,
            @RequestParam PoiStatus request,
            @RequestParam(required = false) String rejectionReason
    ) {
        poiService.updatePoiStatus(id, request, rejectionReason);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePoi(
            @PathVariable Long id
    ) {
        poiService.deletePoi(id);
    }

    @DeleteMapping("/{id}/photo")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePoiPhoto(
            @PathVariable Long id
    ) {
        poiService.deletePoiPhoto(id);
    }

    @PatchMapping("/{id}/location")
    public PoiAdminDTO adjustPoiLocation(
            @PathVariable Long id,
            @RequestBody PointDTO point
    ) {
        return poiService.adjustPoiLocation(id, point);
    }

    @GetMapping("/{id}/history")
    public List<PoiHistoryDTO> getPoiHistory(
            @PathVariable Long id
    ) {
        return poiService.getPoiHistory(id);
    }
}

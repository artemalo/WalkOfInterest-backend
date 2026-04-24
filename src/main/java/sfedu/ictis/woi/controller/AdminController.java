package sfedu.ictis.woi.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;
import sfedu.ictis.woi.api.AdminControllerApi;
import sfedu.ictis.woi.model.UpdateStatusRequest;
import sfedu.ictis.woi.model.dto.PoiDTO;
import sfedu.ictis.woi.service.PoiService;

import java.util.List;

@RestController
@RequestMapping("/api/admin/pois")
@RequiredArgsConstructor
public class AdminController implements AdminControllerApi {
    private final PoiService poiService;

    @Override
    @GetMapping
    public List<PoiDTO> getPoisByStatus(
            @RequestBody UpdateStatusRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "default") String lang
    ) {
        return poiService.getPoisByStatus(request.getStatus(), PageRequest.of(page, size), lang);
    }

    @Override
    @PatchMapping("/{id}/status")
    public void updatePoiStatus(
            @PathVariable Long id,
            @RequestBody UpdateStatusRequest request
    ) {
        poiService.updatePoiStatus(id, request.getStatus());
    }
}
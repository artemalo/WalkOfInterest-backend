package sfedu.ictis.woi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sfedu.ictis.woi.model.MinTimeResponse;
import sfedu.ictis.woi.model.PoisResponse;
import sfedu.ictis.woi.model.RouteSearchRequest;
import sfedu.ictis.woi.model.dto.PointDTO;
import sfedu.ictis.woi.service.PoiService;

@RestController
@RequestMapping("/api/v1/route")
public class PoisController {
    private final PoiService poiService;

    public PoisController(PoiService orchestratorService) {
        this.poiService = orchestratorService;
    }

    @GetMapping("/min-time")
    public ResponseEntity<MinTimeResponse> getMinTime(@RequestParam PointDTO p1, @RequestParam PointDTO p2) {
        return ResponseEntity.ok(new MinTimeResponse(poiService.calculateMinTime(p1, p2)));
    }

    @PostMapping("/search")
    public ResponseEntity<PoisResponse> searchPois(@RequestBody RouteSearchRequest request) {
        PoisResponse response = poiService.getStructuredPois(request);
        return ResponseEntity.ok(response);
    }
}

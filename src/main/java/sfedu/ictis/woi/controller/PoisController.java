package sfedu.ictis.woi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sfedu.ictis.woi.mapper.SearchRequestMapper;
import sfedu.ictis.woi.model.RouteRequest;
import sfedu.ictis.woi.model.RouteResponse;
import sfedu.ictis.woi.model.SearchRequest;
import sfedu.ictis.woi.model.SearchResponse;
import sfedu.ictis.woi.service.OptimizationService;
import sfedu.ictis.woi.service.PoiService;
import sfedu.ictis.woi.service.SearchService;

@RestController
@RequestMapping("/poi")
public class PoisController {
    private final PoiService poiService;
    private final SearchService searchService;
    private final OptimizationService optimizationService;

    public PoisController(PoiService orchestratorService, SearchService searchService, OptimizationService optimizationService) {
        this.poiService = orchestratorService;
        this.searchService = searchService;
        this.optimizationService = optimizationService;
    }

    @GetMapping("/route")
    public ResponseEntity<RouteResponse> getRoute(@RequestBody RouteRequest request) {
        return ResponseEntity.ok(poiService.getRoute(request.getP1(), request.getP2()));
    }

    @PostMapping("/search")
    public ResponseEntity<SearchResponse> search(@RequestBody SearchRequest request) {
        SearchResponse response = searchService.findAllPois(request);

        optimizationService.optimize(response, SearchRequestMapper.toDTO(request));

        return ResponseEntity.ok(response);
    }
}

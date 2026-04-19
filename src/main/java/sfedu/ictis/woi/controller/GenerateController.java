package sfedu.ictis.woi.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sfedu.ictis.woi.api.GenerateControllerApi;
import sfedu.ictis.woi.mapper.SearchRequestMapper;
import sfedu.ictis.woi.model.RouteFromToRequest;
import sfedu.ictis.woi.model.RouteFromToResponse;
import sfedu.ictis.woi.model.SearchRequest;
import sfedu.ictis.woi.model.SearchResponse;
import sfedu.ictis.woi.service.OptimizationService;
import sfedu.ictis.woi.service.PoiService;
import sfedu.ictis.woi.service.SearchService;

@RestController
@RequestMapping("/poi/generate")
public class GenerateController implements GenerateControllerApi {
    private final PoiService poiService;
    private final SearchService searchService;
    private final OptimizationService optimizationService;

    public GenerateController(PoiService orchestratorService, SearchService searchService, OptimizationService optimizationService) {
        this.poiService = orchestratorService;
        this.searchService = searchService;
        this.optimizationService = optimizationService;
    }

    @Override
    @PostMapping("/route")
    public ResponseEntity<RouteFromToResponse> getFromToRoute(@RequestBody RouteFromToRequest request) {
        return ResponseEntity.ok(poiService.getRoute(request.getP1(), request.getP2()));
    }

    @Override
    @PostMapping("/search")
    public ResponseEntity<SearchResponse> search(@Valid @RequestBody SearchRequest request) {
        SearchResponse response = searchService.findAllPois(request);

        optimizationService.optimize(response, SearchRequestMapper.toDTO(request));

        return ResponseEntity.ok(response);
    }
}

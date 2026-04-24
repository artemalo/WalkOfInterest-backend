package sfedu.ictis.woi.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sfedu.ictis.woi.api.GenerateControllerApi;
import sfedu.ictis.woi.mapper.SearchRequestMapper;
import sfedu.ictis.woi.model.RouteFromToRequest;
import sfedu.ictis.woi.model.RouteResponse;
import sfedu.ictis.woi.model.SearchRequest;
import sfedu.ictis.woi.model.SearchResponse;
import sfedu.ictis.woi.model.dto.PointDTO;
import sfedu.ictis.woi.service.OptimizationService;
import sfedu.ictis.woi.service.GenerateService;

import java.util.List;

@RestController
@RequestMapping("/poi/generate")
public class GenerateController implements GenerateControllerApi {
    private final GenerateService generateService;
    private final OptimizationService optimizationService;

    public GenerateController(GenerateService generateService, OptimizationService optimizationService) {
        this.generateService = generateService;
        this.optimizationService = optimizationService;
    }

    @PostMapping("/route")
    public ResponseEntity<RouteResponse> getFromToRoute(@RequestBody RouteFromToRequest request) {
        return ResponseEntity.ok(generateService.getRoute(request.getP1(), request.getP2()));
    }

    @PostMapping("/search")
    public ResponseEntity<SearchResponse> search(@Valid @RequestBody SearchRequest request) {
        SearchResponse response = generateService.findAllPois(request);

        optimizationService.optimize(response, SearchRequestMapper.toDTO(request));

        return ResponseEntity.ok(response);
    }

    @GetMapping("/time")
    public ResponseEntity<Long> getTime(List<PointDTO> request) {
        return null; // TODO
    }
}

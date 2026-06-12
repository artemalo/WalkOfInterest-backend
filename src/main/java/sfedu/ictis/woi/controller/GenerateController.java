package sfedu.ictis.woi.controller;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import sfedu.ictis.woi.api.GenerateControllerApi;
import sfedu.ictis.woi.mapper.SearchRequestMapper;
import sfedu.ictis.woi.model.RouteFromToRequest;
import sfedu.ictis.woi.model.RouteResponse;
import sfedu.ictis.woi.model.SearchRequest;
import sfedu.ictis.woi.model.SearchResponse;
import sfedu.ictis.woi.model.dto.PoiOrderDTO;
import sfedu.ictis.woi.model.dto.PointDTO;
import sfedu.ictis.woi.model.dto.ReorderRequest;
import sfedu.ictis.woi.service.OptimizationService;
import sfedu.ictis.woi.service.GenerateService;
import sfedu.ictis.woi.service.RateLimitType;
import sfedu.ictis.woi.service.RateLimiterService;
import sfedu.ictis.woi.service.UserService;
import sfedu.ictis.woi.util.RateLimitKey;

import java.util.List;

@RestController
@RequestMapping("/api/poi/generate")
public class GenerateController implements GenerateControllerApi {
    private final GenerateService generateService;
    private final OptimizationService optimizationService;

    private final UserService userService;
    private final RateLimiterService rateLimiterService;

    public GenerateController(GenerateService generateService, OptimizationService optimizationService,
                              UserService userService, RateLimiterService rateLimiterService) {
        this.generateService = generateService;
        this.optimizationService = optimizationService;
        this.userService = userService;
        this.rateLimiterService = rateLimiterService;
    }

    @PostMapping("/route")
    public ResponseEntity<RouteResponse> getFromToRoute(
            @RequestBody RouteFromToRequest request
    ) {
        rateLimiterService.consumeOrThrow(RateLimitType.ROUTE_BUILD, RateLimitKey.currentUser(),
                "Слишком много запросов маршрутов за час. Попробуйте позже.");
        return ResponseEntity.ok(generateService.getRoute(request.getP1(), request.getP2()));
    }

    @PostMapping("/search")
    public ResponseEntity<SearchResponse> search(
            @Parameter(hidden = true) Authentication authentication,
            @Valid @RequestBody SearchRequest request
    ) {
        rateLimiterService.consumeOrThrow(RateLimitType.GENERATE_SEARCH, RateLimitKey.currentUser(),
                "Слишком много генераций прогулок за час. Попробуйте позже.");
        SearchResponse response = generateService.findAllPois(request);

        optimizationService.optimize(response, SearchRequestMapper.toDTO(request));

        userService.incrementTrips(authentication);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/time")
    public ResponseEntity<Long> getTime(
            @RequestBody List<PointDTO> request
    ) {
        rateLimiterService.consumeOrThrow(RateLimitType.GENERATE_TIME, RateLimitKey.currentUser(),
                "Слишком много расчётов времени за час. Попробуйте позже.");
        return ResponseEntity.ok(generateService.getTime(request));
    }

    @PostMapping("/reorder")
    public ResponseEntity<List<PoiOrderDTO>> reorder(
            @RequestBody ReorderRequest request
    ) {
        rateLimiterService.consumeOrThrow(RateLimitType.GENERATE_REORDER, RateLimitKey.currentUser(),
                "Слишком много пересчётов маршрута за час. Попробуйте позже.");
        return ResponseEntity.ok(optimizationService.reorder(request));
    }
}
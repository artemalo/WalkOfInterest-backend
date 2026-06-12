package sfedu.ictis.woi.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sfedu.ictis.woi.api.RouteControllerApi;
import sfedu.ictis.woi.model.dto.PointDTO;
import sfedu.ictis.woi.model.dto.RouteDTO;
import sfedu.ictis.woi.service.RateLimitType;
import sfedu.ictis.woi.service.RateLimiterService;
import sfedu.ictis.woi.service.RouteService;
import sfedu.ictis.woi.util.RateLimitKey;

import java.util.List;

@RestController
@RequestMapping("/api/poi/route")
@RequiredArgsConstructor
public class RouteController implements RouteControllerApi {
    private final RouteService routeService;
    private final RateLimiterService rateLimiterService;

    @PostMapping("/routes")
    public ResponseEntity<List<RouteDTO>> getRoutes(
            @RequestBody List<PointDTO> categories
    ) {
        rateLimiterService.consumeOrThrow(RateLimitType.ROUTE_BUILD, RateLimitKey.currentUser(),
                "Слишком много запросов маршрутов за час. Попробуйте позже.");
        return ResponseEntity.ok(routeService.getRoutes(categories));
    }
}

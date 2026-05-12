package sfedu.ictis.woi.controller;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sfedu.ictis.woi.api.RouteControllerApi;
import sfedu.ictis.woi.model.dto.PointDTO;
import sfedu.ictis.woi.model.dto.RouteDTO;
import sfedu.ictis.woi.service.RouteService;
import sfedu.ictis.woi.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/api/poi/route")
@RequiredArgsConstructor
public class RouteController implements RouteControllerApi {
    private final RouteService routeService;
    private final UserService userService;

    @PostMapping("/routes")
    public ResponseEntity<List<RouteDTO>> getRoutes(
            @Parameter(hidden = true) Authentication authentication,
            @RequestBody List<PointDTO> categories
    ) {
        List<RouteDTO> routes = routeService.getRoutes(categories);
        userService.incrementTrips(authentication);

        return ResponseEntity.ok(routes);
    }
}

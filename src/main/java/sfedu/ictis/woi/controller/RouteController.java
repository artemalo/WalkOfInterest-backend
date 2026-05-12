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
import sfedu.ictis.woi.service.RouteService;

import java.util.List;

@RestController
@RequestMapping("/api/poi/route")
@RequiredArgsConstructor
public class RouteController implements RouteControllerApi {
    private final RouteService routeService;

    @PostMapping("/routes")
    public ResponseEntity<List<RouteDTO>> getRoutes(
            @RequestBody List<PointDTO> categories
    ) {
        return ResponseEntity.ok(routeService.getRoutes(categories));
    }
}

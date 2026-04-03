package sfedu.ictis.woi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sfedu.ictis.woi.model.RouteResponse;
import sfedu.ictis.woi.model.SearchRequest;
import sfedu.ictis.woi.model.SearchResponse;
import sfedu.ictis.woi.model.dto.PointDTO;
import sfedu.ictis.woi.service.OptimizationService;
import sfedu.ictis.woi.service.PoiService;
import sfedu.ictis.woi.service.SearchService;

@RestController
@RequestMapping("/")
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
		public ResponseEntity<RouteResponse> getRoute(
		        @RequestParam double lat1,
		        @RequestParam double lon1,
		        @RequestParam double lat2,
		        @RequestParam double lon2) {

		    PointDTO p1 = new PointDTO(lat1, lon1);
		    PointDTO p2 = new PointDTO(lat2, lon2);
        return ResponseEntity.ok(poiService.getRoute(p1, p2));
    }

    @PostMapping("/search")
    public ResponseEntity<SearchResponse> search(@RequestBody SearchRequest request) {
        // 1. Получаем все возможные точки из БД (как ты делал раньше)
        SearchResponse response = searchService.findAllPois(request);

        // 2. Оптимизируем (делаем выбор за пользователя)
        // Передаем лимит времени T из запроса
        optimizationService.optimize(response, request.getMaxTime());

        return ResponseEntity.ok(response);
    }
}

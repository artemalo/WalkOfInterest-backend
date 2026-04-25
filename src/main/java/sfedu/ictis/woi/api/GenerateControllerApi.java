package sfedu.ictis.woi.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import sfedu.ictis.woi.model.RouteFromToRequest;
import sfedu.ictis.woi.model.RouteResponse;
import sfedu.ictis.woi.model.SearchRequest;
import sfedu.ictis.woi.model.SearchResponse;
import sfedu.ictis.woi.model.dto.PointDTO;

import java.util.List;

@Tag(name = "Generate Controller", description = "Поиск точек интереса и маршруты")
@ApiResponses({
        @ApiResponse(responseCode = "503", description = "Проблемы с сервисом")
})
@RequestMapping("/api/poi/generate")
public interface GenerateControllerApi {
    @Operation(
            summary = "Получить маршрут",
            description = "Рассчитывает оптимальный путь между двумя точками"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Маршрут успешно построен"),
            @ApiResponse(responseCode = "400", description = "Некорректные входные данные")
    })
    @PostMapping("/route")
    ResponseEntity<RouteResponse> getFromToRoute(@RequestBody RouteFromToRequest request);

    @Operation(
            summary = "Поиск POI",
            description = "Ищет точки интереса и оптимизирует их список с учетом временных ограничений"
    )
    @ApiResponse(responseCode = "200", description = "Список найденных и отфильтрованных точек")
    @PostMapping("/search")
    ResponseEntity<SearchResponse> search(@RequestBody SearchRequest request);

    @Operation(
            summary = "Получить время из множества точек",
            description = "Рассчитать время оптимального маршрута множества точек"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Время получено"),
            @ApiResponse(responseCode = "400", description = "Некорректные входные данные")
    })
    @GetMapping("/time")
    ResponseEntity<Long> getTime(@RequestBody List<PointDTO> request);
}
package sfedu.ictis.woi.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import sfedu.ictis.woi.model.RouteRequest;
import sfedu.ictis.woi.model.RouteResponse;
import sfedu.ictis.woi.model.SearchRequest;
import sfedu.ictis.woi.model.SearchResponse;

@Tag(name = "POI Controller", description = "Управление точками интереса и маршрутами")
@ApiResponses({
        @ApiResponse(responseCode = "503", description = "Проблемы с сервисом")
})
public interface PoisControllerApi {
    @Operation(
            summary = "Получить маршрут",
            description = "Рассчитывает оптимальный путь между двумя точками"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Маршрут успешно построен"),
            @ApiResponse(responseCode = "400", description = "Некорректные входные данные")
    })
    ResponseEntity<RouteResponse> getRoute(@RequestBody RouteRequest request);

    @Operation(
            summary = "Поиск POI",
            description = "Ищет точки интереса и оптимизирует их список с учетом временных ограничений"
    )
    @ApiResponse(responseCode = "200", description = "Список найденных и отфильтрованных точек")
    ResponseEntity<SearchResponse> search(@RequestBody SearchRequest request);
}
package sfedu.ictis.woi.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import sfedu.ictis.woi.model.RouteFromToRequest;
import sfedu.ictis.woi.model.RouteFromToResponse;
import sfedu.ictis.woi.model.SearchRequest;
import sfedu.ictis.woi.model.SearchResponse;

@Tag(name = "Generate Controller", description = "Поиск точек интереса и маршруты")
@ApiResponses({
        @ApiResponse(responseCode = "503", description = "Проблемы с сервисом")
})
public interface GenerateControllerApi {
    @Operation(
            summary = "Получить маршрут",
            description = "Рассчитывает оптимальный путь между двумя точками"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Маршрут успешно построен"),
            @ApiResponse(responseCode = "400", description = "Некорректные входные данные")
    })
    ResponseEntity<RouteFromToResponse> getFromToRoute(@RequestBody RouteFromToRequest request);

    @Operation(
            summary = "Поиск POI",
            description = "Ищет точки интереса и оптимизирует их список с учетом временных ограничений"
    )
    @ApiResponse(responseCode = "200", description = "Список найденных и отфильтрованных точек")
    ResponseEntity<SearchResponse> search(@RequestBody SearchRequest request);
}
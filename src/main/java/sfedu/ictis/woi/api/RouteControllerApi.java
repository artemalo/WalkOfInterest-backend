package sfedu.ictis.woi.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import sfedu.ictis.woi.model.RouteFromToRequest;
import sfedu.ictis.woi.model.RouteFromToResponse;

@Tag(name = "Route Controller", description = "Построение маршрута")
@ApiResponses({
        @ApiResponse(responseCode = "503", description = "Проблемы с сервисом")
})
public interface RouteControllerApi {
    @Operation(
        summary = "Получить маршрут",
        description = "Рассчитывает оптимальный путь списком точек"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Маршрут успешно построен"),
        @ApiResponse(responseCode = "400", description = "Некорректные входные данные")
    })
    ResponseEntity<RouteFromToResponse> getRoute(@RequestBody RouteFromToRequest request);
}

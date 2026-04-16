package sfedu.ictis.woi.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import sfedu.ictis.woi.model.RouteRequest;
import sfedu.ictis.woi.model.RouteResponse;

@Tag(name = "Categories Controller", description = "Пересчет точек интереса")
@ApiResponses({
        @ApiResponse(responseCode = "503", description = "Проблемы с сервисом")
})
public interface CategoriesControllerApi {
    @Operation(
            summary = "Получить время точек",
            description = "Рассчитать время оптимального маршрута"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Маршрут успешно построен"),
            @ApiResponse(responseCode = "400", description = "Некорректные входные данные")
    })
    ResponseEntity<RouteResponse> getTime(@RequestBody RouteRequest request);
}
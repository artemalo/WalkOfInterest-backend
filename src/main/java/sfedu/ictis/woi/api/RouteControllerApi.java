package sfedu.ictis.woi.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import sfedu.ictis.woi.model.dto.PointDTO;
import sfedu.ictis.woi.model.dto.RouteDTO;

import java.util.List;

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
    @PostMapping("/routes")
    ResponseEntity<List<RouteDTO>> getRoutes(List<PointDTO> categories);
}

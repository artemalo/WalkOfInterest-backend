package sfedu.ictis.woi.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import sfedu.ictis.woi.model.dto.PoiAdminDTO;
import sfedu.ictis.woi.model.entity.PoiStatus;

import java.util.List;

@Tag(name = "Admin", description = "Админ панель POI")
@ApiResponses({
        @ApiResponse(responseCode = "403", description = "Недостаточно прав"),
        @ApiResponse(responseCode = "503", description = "Проблемы с сервисом")
})
public interface AdminControllerApi {
    @Operation(summary = "Список POI с фильтрацией по статусу и пагинацией")
    @GetMapping
    List<PoiAdminDTO> getPoisByStatus(
            @Parameter(description = "Статус точек", example = "PENDING")
            @RequestParam PoiStatus request,

            @Parameter(description = "Номер страницы")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Размер страницы")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "Язык")
            @RequestParam(defaultValue = "default") String lang
    );

    @Operation(summary = "Изменить статус POI")
    @PatchMapping("/{id}/status")
    void updatePoiStatus(
            @PathVariable Long id,

            @Parameter(description = "Новый статус точки")
            @RequestParam PoiStatus request
    );
}

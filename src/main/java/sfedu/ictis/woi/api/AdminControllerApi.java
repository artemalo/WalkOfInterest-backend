package sfedu.ictis.woi.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import sfedu.ictis.woi.model.UpdateStatusRequest;
import sfedu.ictis.woi.model.dto.PoiDTO;

import java.util.List;

@Tag(name = "Admin", description = "Админ панель POI")
@ApiResponses({
        @ApiResponse(responseCode = "503", description = "Проблемы с сервисом")
})
public interface AdminControllerApi {
    @Operation(summary = "Получить POI по статусу (с пагинацией)")
    @GetMapping
    List<PoiDTO> getPoisByStatus(
            @RequestBody UpdateStatusRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "default") String lang
    );

    @Operation(summary = "Изменить статус POI")
    @PatchMapping("/{id}/status")
    void updatePoiStatus(
            @PathVariable Long id,
            @RequestBody UpdateStatusRequest request
    );
}

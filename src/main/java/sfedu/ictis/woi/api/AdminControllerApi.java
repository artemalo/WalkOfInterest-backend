package sfedu.ictis.woi.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import sfedu.ictis.woi.model.dto.PoiAdminDTO;
import sfedu.ictis.woi.model.dto.PoiHistoryDTO;
import sfedu.ictis.woi.model.entity.PoiStatus;

import java.util.List;

@Tag(name = "Admin", description = "Админ панель POI")
@ApiResponses({
        @ApiResponse(responseCode = "403", description = "Недостаточно прав"),
        @ApiResponse(responseCode = "503", description = "Проблемы с сервисом")
})
@RequestMapping("/api/admin/pois")
public interface AdminControllerApi {

    @Operation(summary = "Список POI с фильтрацией по статусу, поиском и пагинацией")
    @GetMapping
    List<PoiAdminDTO> getPoisByStatus(
            @Parameter(description = "Статус точек", example = "PENDING")
            @RequestParam PoiStatus request,

            @Parameter(description = "Номер страницы")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Размер страницы")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "Язык")
            @RequestParam(defaultValue = "default") String lang,

            @Parameter(description = "Строка поиска: имя (подстрока) или числовой ID")
            @RequestParam(required = false) String search,

            @Parameter(description = "Тип владельца: OSM (без пользователя) | USER (создан пользователем)")
            @RequestParam(required = false) String ownerType,

            @Parameter(description = "Широта для поиска в радиусе 150м")
            @RequestParam(required = false) Double lat,

            @Parameter(description = "Долгота для поиска в радиусе 150м")
            @RequestParam(required = false) Double lon
    );

    @Operation(
            summary = "Изменить статус POI",
            description = "При status=REJECTED желательно передать rejectionReason - "
                    + "его покажет пользователю экран 'Мои POI'. При остальных статусах "
                    + "rejectionReason игнорируется и сбрасывается в null"
    )
    @PatchMapping("/{id}/status")
    void updatePoiStatus(
            @PathVariable Long id,

            @Parameter(description = "Новый статус точки")
            @RequestParam PoiStatus request,

            @Parameter(description = "Причина отказа (только при REJECTED)")
            @RequestParam(required = false) String rejectionReason
    );

    @Operation(summary = "Удалить POI (только со статусом REJECTED)")
    @DeleteMapping("/{id}")
    void deletePoi(
            @PathVariable Long id
    );

    @Operation(summary = "Удалить фото POI")
    @DeleteMapping("/{id}/photo")
    void deletePoiPhoto(
            @PathVariable Long id
    );

    @Operation(summary = "История изменений POI")
    @GetMapping("/{id}/history")
    List<PoiHistoryDTO> getPoiHistory(
            @PathVariable Long id
    );
}

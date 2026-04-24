package sfedu.ictis.woi.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import sfedu.ictis.woi.model.dto.PoiAddDTO;
import sfedu.ictis.woi.model.dto.PoiCardDTO;
import sfedu.ictis.woi.model.dto.PoiInfoDTO;

import java.util.List;

@Tag(name = "POI", description = "Взаимодействие с точками интереса")
@ApiResponses({
        @ApiResponse(responseCode = "503", description = "Проблемы с сервисом")
})
@RequestMapping("/api/pois")
public interface PoiControllerApi {
    @Operation(summary = "Получить детальную информацию о POI по ID")
    @GetMapping("/{id}")
    PoiInfoDTO getPoiById(
            @PathVariable Long id,

            @Parameter(description = "Язык ответа (ru/en)")
            @RequestParam(defaultValue = "ru") String lang
    );

    @Operation(summary = "Обновить существующую POI")
    @PutMapping("/{id}")
    PoiInfoDTO updatePoi(
            @PathVariable Long id,
            @RequestBody PoiAddDTO poi
    );

    @Operation(summary = "Получить список POI, созданных текущим пользователем")
    @GetMapping("/my")
    List<PoiCardDTO> getUserPois(
            @RequestParam(defaultValue = "ru") String lang
    );

    @Operation(summary = "Создать новую POI")
    @PostMapping
    PoiInfoDTO createPoi(@RequestBody PoiAddDTO poi);
}

package sfedu.ictis.woi.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import sfedu.ictis.woi.model.dto.PoiDTO;

import java.util.List;

@Tag(name = "POI", description = "Взаимодействие с poi")
@ApiResponses({
        @ApiResponse(responseCode = "503", description = "Проблемы с сервисом")
})
@RequestMapping("/api/pois")
public interface PoiControllerApi {
    @Operation(summary = "Получить POI по id")
    @GetMapping("/{id}")
    PoiDTO getPoiById(@PathVariable Long id);

    @Operation(summary = "Обновить POI (пользовательский)") // TODO может только пользователь владеющий poi
    @PutMapping("/{id}")
    PoiDTO updatePoi(@PathVariable Long id, @RequestBody PoiDTO poi);

    @Operation(summary = "Получить POI текущего пользователя")
    @GetMapping("/my")
    List<PoiDTO> getUserPois();

    @PostMapping
    PoiDTO createPoi(@RequestBody PoiDTO poi);
}

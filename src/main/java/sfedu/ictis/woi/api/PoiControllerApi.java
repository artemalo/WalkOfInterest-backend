package sfedu.ictis.woi.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import sfedu.ictis.woi.model.dto.PoiAddDTO;
import sfedu.ictis.woi.model.dto.PoiCardDTO;
import sfedu.ictis.woi.model.dto.PoiInfoDTO;

import java.util.List;

@Tag(name = "POI", description = "Взаимодействие с poi")
@ApiResponses({
        @ApiResponse(responseCode = "503", description = "Проблемы с сервисом")
})
@RequestMapping("/api/pois")
public interface PoiControllerApi {
    @Operation(summary = "Получить POI по id")
    @GetMapping("/{id}")
    PoiInfoDTO getPoiById(@PathVariable Long id);

    @Operation(summary = "Обновить POI (пользовательский)") // TODO может только пользователь владеющий poi
    @PutMapping("/{id}")
    PoiInfoDTO updatePoi(@PathVariable Long id, @RequestBody PoiAddDTO poi);

    @Operation(summary = "Получить POI текущего пользователя")
    @GetMapping("/my")
    List<PoiCardDTO> getUserPois();

    @PostMapping
    PoiInfoDTO createPoi(@RequestBody PoiAddDTO poi);
}

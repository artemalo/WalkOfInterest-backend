package sfedu.ictis.woi.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import sfedu.ictis.woi.model.dto.*;

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

    @Operation(summary = "Получить список отзывов по POI")
    @GetMapping("/{id}/reviews")
    List<ReviewDTO> getReviewsByPoiId(
            @PathVariable Long id,
            @Parameter(description = "Язык ответа (ru/en)")
            @RequestParam(defaultValue = "ru") String lang
    );

    @Operation(summary = "Создать или обновить отзыв текущего пользователя на POI (один отзыв на пользователя)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Отзыв создан или обновлён"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации"),
            @ApiResponse(responseCode = "401", description = "Не авторизован"),
            @ApiResponse(responseCode = "404", description = "POI не найден")
    })
    @PutMapping("/{id}/reviews/me")
    ReviewDTO upsertMyReview(
            @PathVariable Long id,
            @Valid @RequestBody ReviewRequestDTO request,
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
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "POI создана и отправлена на модерацию"),
            @ApiResponse(responseCode = "401", description = "Не авторизован"),
            @ApiResponse(responseCode = "409", description = "Точка уже существует рядом (без force=true)"),
            @ApiResponse(responseCode = "429", description = "Превышен лимит создания (Retry-After в заголовке)")
    })
    @PostMapping
    PoiInfoDTO createPoi(@RequestBody PoiAddDTO poi);

    @Operation(summary = "Проверить наличие похожих POI рядом")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Результат проверки"),
            @ApiResponse(responseCode = "401", description = "Не авторизован")
    })
    @PostMapping("/check")
    PoiNearbyCheckResponseDTO checkNearby(
            @Valid @RequestBody PoiNearbyCheckRequestDTO request,
            @Parameter(description = "Язык ответа (ru/en)")
            @RequestParam(defaultValue = "ru") String lang
    );

    @Operation(
            summary = "Дополнить существующую POI (merge-семантика)",
            description = "Только добавляет подкатегории к существующим, " +
                    "и заполняет name/description ТОЛЬКО если они пустые. " +
                    "Никогда не затирает уже введённые данные. После операции POI уходит в PENDING."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "POI дополнена и отправлена на повторную модерацию"),
            @ApiResponse(responseCode = "401", description = "Не авторизован"),
            @ApiResponse(responseCode = "403", description = "Нет прав редактировать чужую точку"),
            @ApiResponse(responseCode = "404", description = "POI не найден")
    })
    @PostMapping("/{id}/supplement")
    PoiInfoDTO supplementPoi(
            @PathVariable Long id,
            @RequestBody PoiAddDTO poi
    );
}
package sfedu.ictis.woi.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import sfedu.ictis.woi.model.dto.ReviewReactionRequestDTO;
import sfedu.ictis.woi.model.dto.ReviewReactionResponseDTO;

@Tag(name = "Reviews", description = "Реакции на отзывы (лайки/дизлайки)")
@ApiResponses({
        @ApiResponse(responseCode = "503", description = "Проблемы с сервисом")
})
@RequestMapping("/api/reviews")
public interface ReviewLikeControllerApi {
    @Operation(summary = "Поставить/переключить/снять реакцию пользователя на отзыв " +
            "(один пользователь может поставить максимум одну реакцию: либо LIKE, либо DISLIKE)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Реакция применена; в ответе актуальные счётчики и текущая реакция пользователя"),
            @ApiResponse(responseCode = "400", description = "Некорректный тип реакции"),
            @ApiResponse(responseCode = "401", description = "Не авторизован"),
            @ApiResponse(responseCode = "404", description = "Отзыв не найден")
    })
    @PutMapping("/{reviewId}/reaction")
    ReviewReactionResponseDTO setReaction(
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewReactionRequestDTO request
    );
}
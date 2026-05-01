package sfedu.ictis.woi.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import sfedu.ictis.woi.model.UpdateUsernameRequest;
import sfedu.ictis.woi.model.dto.ReviewDTO;
import sfedu.ictis.woi.model.dto.UserProfileDTO;

import java.util.List;

@Tag(name = "User", description = "Профиль пользователя и его отзывы")
@RequestMapping("/api/users")
public interface UserControllerApi {
    @Operation(summary = "Получить свой профиль")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Профиль получен"),
            @ApiResponse(responseCode = "401", description = "Не авторизован")
    })
    @GetMapping("/me")
    ResponseEntity<UserProfileDTO> getMyProfile(Authentication authentication);

    @Operation(summary = "Получить профиль пользователя по никнейму")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Профиль получен"),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден")
    })
    @GetMapping("/{username}")
    ResponseEntity<UserProfileDTO> getProfileByUsername(
            @Parameter(description = "Никнейм пользователя") @PathVariable String username
    );

    @Operation(summary = "Получить отзывы пользователя по никнейму")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список отзывов"),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден")
    })
    @GetMapping("/{username}/reviews")
    ResponseEntity<List<ReviewDTO>> getReviewsByUsername(
            @Parameter(description = "Никнейм пользователя") @PathVariable String username,
            @Parameter(description = "Язык POI", example = "ru")
            @RequestParam(defaultValue = "default") String lang
    );

    @Operation(summary = "Изменить свой никнейм")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Никнейм обновлён"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации"),
            @ApiResponse(responseCode = "401", description = "Не авторизован"),
            @ApiResponse(responseCode = "409", description = "Никнейм уже занят")
    })
    @PatchMapping("/me")
    ResponseEntity<UserProfileDTO> updateUsername(
            Authentication authentication,
            @Valid @RequestBody UpdateUsernameRequest request
    );
}
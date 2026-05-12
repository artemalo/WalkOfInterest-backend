package sfedu.ictis.woi.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import sfedu.ictis.woi.model.UpdateProfileRequest;
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
    ResponseEntity<UserProfileDTO> getMyProfile(
            @Parameter(hidden = true) Authentication authentication
    );

    @Operation(
            summary = "Обновить информацию профиля",
            description = "Позволяет изменить имя, фамилию и био (о себе) текущего пользователя"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Данные профиля успешно обновлены"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации входных данных"),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован")
    })
    @PatchMapping("/me/info")
    ResponseEntity<UserProfileDTO> updateProfileInfo(
            @Parameter(hidden = true) Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request
    );

    @Operation(
            summary = "Загрузить фото профиля",
            description = "Позволяет текущему пользователю загрузить или заменить аватар профиля"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Фото профиля успешно обновлено"),
            @ApiResponse(responseCode = "400", description = "Некорректный файл или ошибка валидации"),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован"),
            @ApiResponse(responseCode = "413", description = "Файл слишком большой"),
            @ApiResponse(responseCode = "415", description = "Неподдерживаемый тип файла")
    })
    @PostMapping(value = "/me/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    ResponseEntity<UserProfileDTO> uploadPhoto(
            @Parameter(hidden = true) Authentication authentication,

            @Parameter(description = "Файл изображения (jpg, jpeg, png, webp)", required = true)
            @RequestParam("photo") MultipartFile photo
    );


    @Operation(summary = "Получить профиль пользователя по никнейму")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Профиль получен"),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден")
    })
    @GetMapping("/{username}")
    ResponseEntity<UserProfileDTO> getProfileByUsername(
            @Parameter(description = "Никнейм пользователя") @PathVariable String username
    );

    @Operation(summary = "Получить отзывы пользователя по никнейму (с реакцией текущего пользователя)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список отзывов"),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден")
    })
    @GetMapping("/{username}/reviews")
    ResponseEntity<List<ReviewDTO>> getReviewsByUsername(
            @Parameter(description = "Никнейм пользователя") @PathVariable String username,
            @Parameter(hidden = true) Authentication authentication,
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
            @Parameter(hidden = true) Authentication authentication,
            @Valid @RequestBody UpdateUsernameRequest request
    );
}
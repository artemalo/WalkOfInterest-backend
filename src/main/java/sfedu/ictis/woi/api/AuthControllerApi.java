package sfedu.ictis.woi.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import sfedu.ictis.woi.model.*;

@Tag(
        name = "Auth",
        description = "Аутентификация, регистрация и управление пользовательскими сессиями"
)
@RequestMapping("/api/auth")
public interface AuthControllerApi {

    @Operation(
            summary = "Регистрация пользователя",
            description = "Создаёт нового пользователя и возвращает access/refresh токены"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Пользователь успешно зарегистрирован"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации данных"),
            @ApiResponse(responseCode = "409", description = "Пользователь уже существует")
    })
    @PostMapping("/register")
    ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request,

            @Parameter(hidden = true)
            HttpServletRequest httpRequest
    );

    @Operation(
            summary = "Вход в систему",
            description = "Аутентифицирует пользователя и возвращает access/refresh токены"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Успешная аутентификация"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации"),
            @ApiResponse(responseCode = "401", description = "Неверный email или пароль"),
            @ApiResponse(responseCode = "429", description = "Слишком много попыток входа")
    })
    @PostMapping("/login")
    ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,

            @Parameter(hidden = true)
            HttpServletRequest httpRequest
    );

    @Operation(
            summary = "Обновить access token",
            description = "Возвращает новую пару access/refresh токенов по refresh token"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Токены успешно обновлены"),
            @ApiResponse(responseCode = "401", description = "Невалидный или просроченный refresh token")
    })
    @PostMapping("/refresh")
    ResponseEntity<AuthResponse> refresh(
            @Valid @RequestBody RefreshRequest request
    );

    @Operation(
            summary = "Выход из текущей сессии",
            description = "Инвалидирует текущий refresh token"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Успешный logout"),
            @ApiResponse(responseCode = "401", description = "Невалидный refresh token")
    })
    @PostMapping("/logout")
    ResponseEntity<Void> logout(
            @Valid @RequestBody LogoutRequest request
    );

    @Operation(
            summary = "Выход со всех устройств",
            description = "Удаляет все refresh token текущего пользователя"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Все сессии успешно завершены"),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован")
    })
    @PostMapping("/logout-all")
    ResponseEntity<Void> logoutAll(
            @Parameter(hidden = true)
            Authentication authentication
    );
}
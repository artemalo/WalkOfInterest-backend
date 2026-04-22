package sfedu.ictis.woi.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import io.swagger.v3.oas.annotations.tags.Tag;
import sfedu.ictis.woi.model.*;


@Tag(name = "Auth", description = "Аутентификация и управление сессиями")
@RequestMapping("/api/auth")
public interface AuthControllerApi {
    @Operation(summary = "Регистрация пользователя")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Успешная регистрация"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации"),
            @ApiResponse(responseCode = "409", description = "Пользователь уже существует")
    })
    @PostMapping("/register")
    ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request);


    @Operation(summary = "Вход в систему")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Успешный вход"),
            @ApiResponse(responseCode = "401", description = "Неверные данные")
    })
    @PostMapping("/login")
    ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request);


    @Operation(summary = "Обновление access token по refresh token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Токен обновлён"),
            @ApiResponse(responseCode = "401", description = "Невалидный refresh token")
    })
    @PostMapping("/refresh")
    ResponseEntity<AuthResponse> refresh(@RequestBody RefreshRequest request);


    @Operation(summary = "Выход (одна сессия)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Успешный logout"),
            @ApiResponse(responseCode = "401", description = "Невалидный refresh token")
    })
    @PostMapping("/logout")
    ResponseEntity<Void> logout(@RequestBody LogoutRequest request);


    @Operation(summary = "Выход со всех устройств")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Все сессии завершены"),
            @ApiResponse(responseCode = "401", description = "Не авторизован")
    })
    @PostMapping("/logout-all")
    ResponseEntity<Void> logoutAll(Authentication authentication);
}
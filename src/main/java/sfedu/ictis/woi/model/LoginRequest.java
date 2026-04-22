package sfedu.ictis.woi.model;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Почта обязательна")
        String email,

        @NotBlank(message = "Пароль обязателен")
        String password
) {}
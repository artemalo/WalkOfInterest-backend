package sfedu.ictis.woi.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUsernameRequest(
        @NotBlank(message = "Никнейм не может быть пустым")
        @Size(min = 3, max = 32, message = "Никнейм должен быть от 3 до 32 символов")
        @Pattern(regexp = "^[A-Za-z0-9_]+$",
                message = "Никнейм может содержать только латиницу, цифры и нижнее подчёркивание")
        String username
) {
}
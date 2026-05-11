package sfedu.ictis.woi.model;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 64, message = "Имя не должно превышать 64 символа")
        String firstName,

        @Size(max = 64, message = "Фамилия не должна превышать 64 символа")
        String lastName,

        @Size(max = 255, message = "Описание 'О себе' не должно превышать 255 символов")
        String bio
) {
}
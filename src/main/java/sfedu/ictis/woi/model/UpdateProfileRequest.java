package sfedu.ictis.woi.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank(message = "Имя не может быть пустым")
        @Size(max = 64, message = "Имя не должно превышать 64 символа")
        String firstName,

        @NotBlank(message = "Фамилия не может быть пустым")
        @Size(max = 64, message = "Фамилия не должна превышать 64 символа")
        String lastName,

        @Size(max = 255, message = "Описание 'О себе' не должно превышать 255 символов")
        String bio
) {
        public UpdateProfileRequest {
                if (firstName != null) firstName = firstName.trim();
                if (lastName != null) lastName = lastName.trim();
                if (bio != null) bio = bio.trim();
        }
}
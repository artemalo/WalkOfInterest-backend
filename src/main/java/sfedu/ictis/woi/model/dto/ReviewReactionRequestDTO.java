package sfedu.ictis.woi.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewReactionRequestDTO {
    @NotNull(message = "Тип реакции обязателен")
    private ReactionType type;
}
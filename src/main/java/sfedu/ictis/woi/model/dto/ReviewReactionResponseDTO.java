package sfedu.ictis.woi.model.dto;

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
public class ReviewReactionResponseDTO {
    private Long reviewId;
    private int likes;
    private int dislikes;

    private ReactionType myReaction;
}
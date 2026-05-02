package sfedu.ictis.woi.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewDTO {
    private Long id;
    private String authorUsername;
    private String authorAvatarUrl;
    private Long poiId;
    private String poiName;
    private String content;
    private Integer rating;
    private Integer likes;
    private Integer dislikes;

    private ReactionType myReaction;
    private LocalDateTime createdAt;
}
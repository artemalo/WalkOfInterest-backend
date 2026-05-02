package sfedu.ictis.woi.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import sfedu.ictis.woi.api.ReviewLikeControllerApi;
import sfedu.ictis.woi.model.dto.ReviewReactionRequestDTO;
import sfedu.ictis.woi.model.dto.ReviewReactionResponseDTO;
import sfedu.ictis.woi.service.ReviewLikeService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reviews")
public class ReviewLikeController implements ReviewLikeControllerApi {
    private final ReviewLikeService reviewLikeService;

    @PutMapping("/{reviewId}/reaction")
    public ReviewReactionResponseDTO setReaction(
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewReactionRequestDTO request
    ) {
        return reviewLikeService.setReaction(
                reviewId,
                request.getType(),
                SecurityContextHolder.getContext().getAuthentication()
        );
    }
}
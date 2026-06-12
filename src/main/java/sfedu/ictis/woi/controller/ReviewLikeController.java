package sfedu.ictis.woi.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import sfedu.ictis.woi.api.ReviewLikeControllerApi;
import sfedu.ictis.woi.model.dto.ReviewReactionRequestDTO;
import sfedu.ictis.woi.model.dto.ReviewReactionResponseDTO;
import sfedu.ictis.woi.service.RateLimitType;
import sfedu.ictis.woi.service.RateLimiterService;
import sfedu.ictis.woi.service.ReviewLikeService;
import sfedu.ictis.woi.util.RateLimitKey;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reviews")
public class ReviewLikeController implements ReviewLikeControllerApi {
    private final ReviewLikeService reviewLikeService;
    private final RateLimiterService rateLimiterService;

    @PutMapping("/{reviewId}/reaction")
    public ReviewReactionResponseDTO setReaction(
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewReactionRequestDTO request
    ) {
        rateLimiterService.consumeOrThrow(RateLimitType.REVIEW_REACTION, RateLimitKey.currentUser(),
                "Слишком много реакций за час. Попробуйте позже.");
        return reviewLikeService.setReaction(
                reviewId,
                request.getType(),
                SecurityContextHolder.getContext().getAuthentication()
        );
    }
}
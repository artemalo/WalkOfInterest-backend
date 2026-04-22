package sfedu.ictis.woi.model;

import io.swagger.v3.oas.annotations.media.Schema;

public record AuthResponse(
        @Schema(example = "jwt-access-token")
        String accessToken,

        @Schema(example = "refresh-token")
        String refreshToken
) {}
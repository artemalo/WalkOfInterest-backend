package sfedu.ictis.woi.controller;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import sfedu.ictis.woi.api.UserControllerApi;
import sfedu.ictis.woi.model.UpdateProfileRequest;
import sfedu.ictis.woi.model.UpdateUsernameRequest;
import sfedu.ictis.woi.model.dto.ReviewDTO;
import sfedu.ictis.woi.model.dto.UserProfileDTO;
import sfedu.ictis.woi.service.RateLimitType;
import sfedu.ictis.woi.service.RateLimiterService;
import sfedu.ictis.woi.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController implements UserControllerApi {
    private final UserService userService;
    private final RateLimiterService rateLimiterService;

    @GetMapping("/me")
    public ResponseEntity<UserProfileDTO> getMyProfile(
            @Parameter(hidden = true) Authentication authentication
    ) {
        return ResponseEntity.ok(userService.getMyProfile(authentication));
    }

    @PatchMapping("/me")
    public ResponseEntity<UserProfileDTO> updateUsername(
            @Parameter(hidden = true) Authentication authentication,
            @Valid @RequestBody UpdateUsernameRequest request
    ) {
        rateLimiterService.consumeOrThrow(RateLimitType.USER_RENAME, authentication.getName(),
                "Слишком частая смена никнейма. Попробуйте завтра.");
        return ResponseEntity.ok(userService.updateUsername(authentication, request.username()));
    }

    @PatchMapping("/me/info")
    public ResponseEntity<UserProfileDTO> updateProfileInfo(
            @Parameter(hidden = true) Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        rateLimiterService.consumeOrThrow(RateLimitType.USER_INFO_UPDATE, authentication.getName(),
                "Слишком много изменений профиля за час. Попробуйте позже.");
        return ResponseEntity.ok(userService.updateProfileInfo(authentication, request));
    }

    @PostMapping(value = "/me/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserProfileDTO> uploadPhoto(
            @Parameter(hidden = true) Authentication authentication,
            @RequestParam("photo") MultipartFile photo
    ) {
        rateLimiterService.consumeOrThrow(RateLimitType.USER_PHOTO_UPLOAD, authentication.getName(),
                "Превышен дневной лимит загрузки фото профиля. Попробуйте завтра.");
        return ResponseEntity.ok(userService.uploadPhoto(authentication, photo));
    }


    @GetMapping("/{username}")
    public ResponseEntity<UserProfileDTO> getProfileByUsername(@PathVariable String username) {
        return ResponseEntity.ok(userService.getProfileByUsername(username));
    }

    @GetMapping("/{username}/reviews")
    public ResponseEntity<List<ReviewDTO>> getReviewsByUsername(
            @PathVariable String username,
            @Parameter(hidden = true) Authentication authentication,
            @RequestParam(defaultValue = "default") String lang
    ) {
        return ResponseEntity.ok(userService.getReviewsByUsername(username, authentication, lang));
    }
}
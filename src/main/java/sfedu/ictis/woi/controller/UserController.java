package sfedu.ictis.woi.controller;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import sfedu.ictis.woi.api.UserControllerApi;
import sfedu.ictis.woi.model.UpdateProfileRequest;
import sfedu.ictis.woi.model.UpdateUsernameRequest;
import sfedu.ictis.woi.model.dto.ReviewDTO;
import sfedu.ictis.woi.model.dto.UserProfileDTO;
import sfedu.ictis.woi.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController implements UserControllerApi {
    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserProfileDTO> getMyProfile(
            @Parameter(hidden = true) Authentication authentication
    ) {
        return ResponseEntity.ok(userService.getMyProfile(authentication));
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

    @PatchMapping("/me")
    public ResponseEntity<UserProfileDTO> updateUsername(
            @Parameter(hidden = true) Authentication authentication,
            @Valid @RequestBody UpdateUsernameRequest request
    ) {
        return ResponseEntity.ok(userService.updateUsername(authentication, request.username()));
    }

    @PatchMapping("/me/info")
    public ResponseEntity<UserProfileDTO> updateProfileInfo(
            @Parameter(hidden = true) Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return ResponseEntity.ok(userService.updateProfileInfo(authentication, request));
    }
}
package sfedu.ictis.woi.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import sfedu.ictis.woi.api.UserControllerApi;
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
    public ResponseEntity<UserProfileDTO> getMyProfile(Authentication authentication) {
        return ResponseEntity.ok(userService.getMyProfile(authentication));
    }

    @GetMapping("/{username}")
    public ResponseEntity<UserProfileDTO> getProfileByUsername(@PathVariable String username) {
        return ResponseEntity.ok(userService.getProfileByUsername(username));
    }

    @GetMapping("/{username}/reviews")
    public ResponseEntity<List<ReviewDTO>> getReviewsByUsername(
            @PathVariable String username,
            @RequestParam(defaultValue = "default") String lang
    ) {
        return ResponseEntity.ok(userService.getReviewsByUsername(username, lang));
    }

    @PatchMapping("/me")
    public ResponseEntity<UserProfileDTO> updateUsername(
            Authentication authentication,
            @Valid @RequestBody UpdateUsernameRequest request
    ) {
        return ResponseEntity.ok(userService.updateUsername(authentication, request.username()));
    }
}
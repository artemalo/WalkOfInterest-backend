package sfedu.ictis.woi.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import sfedu.ictis.woi.api.AuthControllerApi;
import sfedu.ictis.woi.model.*;
import sfedu.ictis.woi.service.AuthService;
import sfedu.ictis.woi.service.RateLimitType;
import sfedu.ictis.woi.service.RateLimiterService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController implements AuthControllerApi {
    private final AuthService authService;
    private final RateLimiterService rateLimiterService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request,
            jakarta.servlet.http.HttpServletRequest httpRequest) {

        String ip = httpRequest.getRemoteAddr();
        return ResponseEntity.ok(authService.register(request, ip));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            jakarta.servlet.http.HttpServletRequest httpRequest) {

        String ip = httpRequest.getRemoteAddr();
        return ResponseEntity.ok(authService.login(request, ip));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @Valid @RequestBody RefreshRequest request,
            jakarta.servlet.http.HttpServletRequest httpRequest) {

        rateLimiterService.consumeOrThrow(RateLimitType.AUTH_REFRESH, httpRequest.getRemoteAddr(),
                "Слишком много обновлений токена. Попробуйте позже.");
        return ResponseEntity.ok(authService.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll(Authentication authentication) {
        authService.logoutAll(authentication);
        return ResponseEntity.ok().build();
    }
}
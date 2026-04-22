package sfedu.ictis.woi.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import sfedu.ictis.woi.config.ApplicationConfig;
import sfedu.ictis.woi.exception.*;
import sfedu.ictis.woi.model.AuthResponse;
import sfedu.ictis.woi.model.LoginRequest;
import sfedu.ictis.woi.model.RegisterRequest;
import sfedu.ictis.woi.model.entity.RefreshTokenEntity;
import sfedu.ictis.woi.model.entity.UserEntity;
import sfedu.ictis.woi.model.entity.UserRole;
import sfedu.ictis.woi.repository.RefreshTokenRepository;
import sfedu.ictis.woi.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenRepository refreshTokenRepository;

    private final ApplicationConfig applicationConfig;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new UserAlreadyExistsException(request.username());
        }

        UserEntity user = new UserEntity();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setRole(UserRole.USER);

        userRepository.save(user);

        var userDetails = applicationConfig.userDetailsService().loadUserByUsername(user.getUsername());

        String accessToken  = jwtService.generateToken(userDetails);
        String refreshToken = refreshTokenService.createRefreshToken(user.getUsername()).getToken();

        return new AuthResponse(accessToken, refreshToken);
    }

    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );
        } catch (BadCredentialsException e) {
            throw new InvalidCredentialsException();
        }

        var userDetails = applicationConfig.userDetailsService().loadUserByUsername(request.username());

        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = refreshTokenService.createRefreshToken(request.username()).getToken();

        return new AuthResponse(accessToken, refreshToken);
    }

    public AuthResponse refresh(String refreshToken) {
        RefreshTokenEntity token = refreshTokenService.verify(refreshToken);

        var userDetails = applicationConfig.userDetailsService()
                .loadUserByUsername(token.getUser().getUsername());

        String newAccessToken = jwtService.generateToken(userDetails);

        return new AuthResponse(newAccessToken, refreshToken);
    }

    public void logout(String refreshToken) {
        RefreshTokenEntity token = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new BadTokenException("Invalid refresh token"));

        refreshTokenRepository.delete(token);
    }

    @Transactional
    public void logoutAll(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName() == null) {
            throw new InvalidCredentialsException();
        }

        String username = authentication.getName();

        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        refreshTokenRepository.deleteByUser(user);
    }
}
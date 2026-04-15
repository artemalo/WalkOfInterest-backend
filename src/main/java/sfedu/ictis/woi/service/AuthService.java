package sfedu.ictis.woi.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import sfedu.ictis.woi.config.ApplicationConfig;
import sfedu.ictis.woi.exception.BaseException;
import sfedu.ictis.woi.exception.UserAlreadyExistsException;
import sfedu.ictis.woi.exception.InvalidCredentialsException;
import sfedu.ictis.woi.model.AuthResponse;
import sfedu.ictis.woi.model.LoginRequest;
import sfedu.ictis.woi.model.RegisterRequest;
import sfedu.ictis.woi.model.entity.UserEntity;
import sfedu.ictis.woi.model.entity.UserRole;
import sfedu.ictis.woi.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    private final ApplicationConfig applicationConfig;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new UserAlreadyExistsException(request.username());
        }

        if (request.password().length() < 8) {
            throw new BaseException("Пароль слишком короткий", "WEAK_PASSWORD");
        }

        UserEntity user = new UserEntity();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setRole(UserRole.USER);

        userRepository.save(user);

        var userDetails = applicationConfig.userDetailsService().loadUserByUsername(user.getUsername());
        String jwtToken = jwtService.generateToken(userDetails);

        return new AuthResponse(jwtToken);
    }

    public AuthResponse login(LoginRequest request) {
        var user = userRepository.findByUsername(request.username())
                .orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        // Spring Security проверяет пароль и логин
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );
        } catch (BadCredentialsException e) {
            throw new InvalidCredentialsException();
        }

        var userDetails = applicationConfig.userDetailsService().loadUserByUsername(request.username());
        String jwtToken = jwtService.generateToken(userDetails);

        return new AuthResponse(jwtToken);
    }
}
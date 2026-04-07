package sfedu.ictis.woi.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import sfedu.ictis.woi.model.entity.UserEntity;
import sfedu.ictis.woi.model.entity.UserRole;
import sfedu.ictis.woi.repository.UserRepository;

@Service
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
//  private final JwtService jwtService; // позже

//  private final NotifierService notifierService; // почта

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public String register(String username, String password) {

        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalStateException("Пользователь уже существует");
        }

        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(UserRole.USER);

        userRepository.save(user);

//      String token = jwtService.generateToken(username);
        return "token";

//            log.error("Ошибка регистрации", e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Ошибка на сервере");
//        }
    }

    public String login(String username, String password) {
        UserEntity user = userRepository.findByUsername(username).orElseThrow();

//        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
//            throw new IllegalStateException("Неверное имя или пароль");
//        }

//        String token = jwtService.generateToken(username);
        return "token";
    }

    public String updateName(Long userId, String newName) {
        return "update";
    }
}
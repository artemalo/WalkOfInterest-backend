package sfedu.ictis.woi.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sfedu.ictis.woi.exception.BadTokenException;
import sfedu.ictis.woi.exception.ResourceNotFoundException;
import sfedu.ictis.woi.model.entity.RefreshTokenEntity;
import sfedu.ictis.woi.model.entity.UserEntity;
import sfedu.ictis.woi.repository.RefreshTokenRepository;
import sfedu.ictis.woi.repository.UserRepository;

import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Value("${jwt.refresh.expiration}")
    private long REFRESH_EXPIRATION;

    @Transactional
    public RefreshTokenEntity createRefreshToken(String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));

        RefreshTokenEntity token = new RefreshTokenEntity();
        token.setUser(user);
        token.setToken(UUID.randomUUID().toString());
        token.setExpiryDate(new Date(System.currentTimeMillis() + REFRESH_EXPIRATION));

        return refreshTokenRepository.save(token); // in the future: save encoded
    }

    @Transactional
    public RefreshTokenEntity verify(String token) {
        RefreshTokenEntity refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new BadTokenException("Невалидный refresh токен"));

        if (refreshToken.getExpiryDate().before(new Date())) {
            refreshTokenRepository.delete(refreshToken);
            throw new BadTokenException("Срок действия refresh токена истек");
        }

        return refreshToken;
    }
}
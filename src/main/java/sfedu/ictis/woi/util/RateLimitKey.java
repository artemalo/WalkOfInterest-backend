package sfedu.ictis.woi.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Ключ для рейт-лимитера: имя авторизованного пользователя.
 * Эндпоинты под защитой Spring Security, поэтому "anonymous" здесь
 * практически недостижим, но оставлен как безопасный фолбэк.
 */
public final class RateLimitKey {
    private RateLimitKey() {
    }

    public static String currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.getName() != null) ? auth.getName() : "anonymous";
    }
}

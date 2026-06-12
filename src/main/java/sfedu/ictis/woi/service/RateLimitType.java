package sfedu.ictis.woi.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Duration;

@Getter
@RequiredArgsConstructor
public enum RateLimitType {
    // POI: правки идут на модерацию
    POI_UPDATE(10, Duration.ofHours(1)),
    POI_SUPPLEMENT(10, Duration.ofHours(1)),
    POI_CHECK_NEARBY(60, Duration.ofHours(1)),
    POI_PHOTO_UPLOAD(10, Duration.ofHours(24)),

    // Отзывы и реакции
    REVIEW_UPSERT(10, Duration.ofHours(1)),
    REVIEW_REACTION(120, Duration.ofHours(1)),

    // Профиль
    USER_RENAME(5, Duration.ofHours(24)),
    USER_INFO_UPDATE(10, Duration.ofHours(1)),
    USER_PHOTO_UPLOAD(5, Duration.ofHours(24)),

    // Маршрутизация и генерация прогулок
    ROUTE_BUILD(60, Duration.ofHours(1)),
    GENERATE_SEARCH(20, Duration.ofHours(1)),
    GENERATE_TIME(60, Duration.ofHours(1)),
    GENERATE_REORDER(30, Duration.ofHours(1)),

    // Обновление токена
    AUTH_REFRESH(30, Duration.ofMinutes(15));

    private final long capacity;
    private final Duration period;
}

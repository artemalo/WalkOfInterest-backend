package sfedu.ictis.woi.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


@Service
public class RateLimiterService {
    private final long poiCreateCapacity;
    private final long poiCreatePeriodHours;

    private final ConcurrentMap<Long, Bucket> poiCreateBuckets = new ConcurrentHashMap<>();

    public RateLimiterService(
            @Value("${app.rate-limit.poi-create.capacity:5}") long poiCreateCapacity,
            @Value("${app.rate-limit.poi-create.period-hours:24}") long poiCreatePeriodHours
    ) {
        this.poiCreateCapacity = poiCreateCapacity;
        this.poiCreatePeriodHours = poiCreatePeriodHours;
    }

    /**
     * @return true создание разрешено,
     *         false лимит исчерпан.
     */
    public boolean tryConsumePoiCreate(Long userId) {
        Bucket bucket = poiCreateBuckets.computeIfAbsent(userId, _ -> newPoiCreateBucket());
        return bucket.tryConsume(1);
    }

    /**
     * Сколько токенов осталось у пользователя
     */
    public long getPoiCreateRemaining(Long userId) {
        Bucket bucket = poiCreateBuckets.computeIfAbsent(userId, _ -> newPoiCreateBucket());
        return bucket.getAvailableTokens();
    }

    /**
     * Через сколько секунд освободится хотя бы один токен
     */
    public long getPoiCreateRetryAfterSeconds(Long userId) {
        Bucket bucket = poiCreateBuckets.computeIfAbsent(userId, _ -> newPoiCreateBucket());
        long nanos = bucket.estimateAbilityToConsume(1).getNanosToWaitForRefill();
        return Math.max(1L, Duration.ofNanos(nanos).toSeconds());
    }

    private Bucket newPoiCreateBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(poiCreateCapacity)
                .refillIntervally(poiCreateCapacity, Duration.ofHours(poiCreatePeriodHours))
                .build();
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}
package sfedu.ictis.woi.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import sfedu.ictis.woi.exception.RateLimitExceededException;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


@Service
public class RateLimiterService {
    private final long poiCreateCapacity;
    private final long poiCreatePeriodHours;
    private final ConcurrentMap<Long, Bucket> poiCreateBuckets = new ConcurrentHashMap<>();

    private final long registerCapacity;
    private final long registerPeriodHours;
    private final ConcurrentMap<String, Bucket> registerBuckets = new ConcurrentHashMap<>();

    private final long loginCapacity;
    private final long loginPeriodMinutes;
    private final ConcurrentMap<String, Bucket> loginBuckets = new ConcurrentHashMap<>();

    private final long adminDeleteCapacity;
    private final long adminDeletePeriodHours;
    private final ConcurrentMap<String, Bucket> adminDeleteBuckets = new ConcurrentHashMap<>();

    private final long adminModifyCapacity;
    private final long adminModifyPeriodHours;
    private final ConcurrentMap<String, Bucket> adminModifyBuckets = new ConcurrentHashMap<>();

    public RateLimiterService(
            @Value("${app.rate-limit.poi-create.capacity}") long poiCreateCapacity,
            @Value("${app.rate-limit.poi-create.period-hours}") long poiCreatePeriodHours,
            @Value("${app.rate-limit.register.capacity}") long registerCapacity,
            @Value("${app.rate-limit.register.period-hours}") long registerPeriodHours,
            @Value("${app.rate-limit.login.capacity}") long loginCapacity,
            @Value("${app.rate-limit.login.period-minutes}") long loginPeriodMinutes,
            @Value("${app.rate-limit.admin-delete.capacity:10}") long adminDeleteCapacity,
            @Value("${app.rate-limit.admin-delete.period-hours:24}") long adminDeletePeriodHours,
            @Value("${app.rate-limit.admin-modify.capacity:30}") long adminModifyCapacity,
            @Value("${app.rate-limit.admin-modify.period-hours:1}") long adminModifyPeriodHours
    ) {
        this.poiCreateCapacity = poiCreateCapacity;
        this.poiCreatePeriodHours = poiCreatePeriodHours;
        this.registerCapacity = registerCapacity;
        this.registerPeriodHours = registerPeriodHours;
        this.loginCapacity = loginCapacity;
        this.loginPeriodMinutes = loginPeriodMinutes;
        this.adminDeleteCapacity = adminDeleteCapacity;
        this.adminDeletePeriodHours = adminDeletePeriodHours;
        this.adminModifyCapacity = adminModifyCapacity;
        this.adminModifyPeriodHours = adminModifyPeriodHours;
    }

    public boolean tryConsumeRegister(String ip) {
        Bucket bucket = registerBuckets.computeIfAbsent(ip, _ ->
                newBucket(registerCapacity, Duration.ofHours(registerPeriodHours)));
        return bucket.tryConsume(1);
    }

    public boolean tryConsumeLogin(String ip) {
        Bucket bucket = loginBuckets.computeIfAbsent(ip, _ ->
                newBucket(loginCapacity, Duration.ofMinutes(loginPeriodMinutes)));
        return bucket.tryConsume(1);
    }

    public boolean tryConsumePoiCreate(Long userId) {
        Bucket bucket = poiCreateBuckets.computeIfAbsent(userId, _ ->
                newBucket(poiCreateCapacity, Duration.ofHours(poiCreatePeriodHours)));
        return bucket.tryConsume(1);
    }

    private final ConcurrentMap<String, Bucket> apiBuckets = new ConcurrentHashMap<>();

    /**
     * Универсальная проверка лимита. Бросает {@link RateLimitExceededException} (429),
     * если лимит исчерпан. Ключ — username для авторизованных, IP для анонимных.
     */
    public void consumeOrThrow(RateLimitType type, String key, String message) {
        Bucket bucket = apiBuckets.computeIfAbsent(type.name() + ":" + key, _ ->
                newBucket(type.getCapacity(), type.getPeriod()));
        if (!bucket.tryConsume(1)) {
            throw new RateLimitExceededException(message, getRetryAfter(bucket));
        }
    }

    public boolean tryConsumeAdminDelete(String username) {
        Bucket bucket = adminDeleteBuckets.computeIfAbsent(username, _ ->
                newBucket(adminDeleteCapacity, Duration.ofHours(adminDeletePeriodHours)));
        return bucket.tryConsume(1);
    }

    public boolean tryConsumeAdminModify(String username) {
        Bucket bucket = adminModifyBuckets.computeIfAbsent(username, _ ->
                newBucket(adminModifyCapacity, Duration.ofHours(adminModifyPeriodHours)));
        return bucket.tryConsume(1);
    }

    public long getAdminDeleteRetryAfterSeconds(String username) {
        Bucket bucket = adminDeleteBuckets.get(username);
        return getRetryAfter(bucket);
    }

    public long getAdminModifyRetryAfterSeconds(String username) {
        Bucket bucket = adminModifyBuckets.get(username);
        return getRetryAfter(bucket);
    }

    public long getPoiCreateRetryAfterSeconds(Long userId) {
        Bucket bucket = poiCreateBuckets.get(userId);
        return getRetryAfter(bucket);
    }

    public long getRegisterRetryAfterSeconds(String ip) {
        Bucket bucket = registerBuckets.get(ip);
        return getRetryAfter(bucket);
    }

    public long getLoginRetryAfterSeconds(String ip) {
        Bucket bucket = loginBuckets.get(ip);
        return getRetryAfter(bucket);
    }

    private long getRetryAfter(Bucket bucket) {
        if (bucket == null) return 0L;

        if (bucket.getAvailableTokens() > 0) return 0L;

        long nanos = bucket.estimateAbilityToConsume(1).getNanosToWaitForRefill();

        if (nanos <= 0) return 0L;

        return Math.max(1L, Duration.ofNanos(nanos).toSeconds());
    }

    private Bucket newBucket(long capacity, Duration period) {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(capacity)
                        .refillIntervally(capacity, period)
                        .build())
                .build();
    }
}
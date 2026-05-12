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

    private final long registerCapacity;
    private final long registerPeriodHours;
    private final ConcurrentMap<String, Bucket> registerBuckets = new ConcurrentHashMap<>();

    private final long loginCapacity;
    private final long loginPeriodMinutes;
    private final ConcurrentMap<String, Bucket> loginBuckets = new ConcurrentHashMap<>();

    public RateLimiterService(
            @Value("${app.rate-limit.poi-create.capacity}") long poiCreateCapacity,
            @Value("${app.rate-limit.poi-create.period-hours}") long poiCreatePeriodHours,
            @Value("${app.rate-limit.register.capacity}") long registerCapacity,
            @Value("${app.rate-limit.register.period-hours}") long registerPeriodHours,
            @Value("${app.rate-limit.login.capacity}") long loginCapacity,
            @Value("${app.rate-limit.login.period-minutes}") long loginPeriodMinutes
    ) {
        this.poiCreateCapacity = poiCreateCapacity;
        this.poiCreatePeriodHours = poiCreatePeriodHours;
        this.registerCapacity = registerCapacity;
        this.registerPeriodHours = registerPeriodHours;
        this.loginCapacity = loginCapacity;
        this.loginPeriodMinutes = loginPeriodMinutes;
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
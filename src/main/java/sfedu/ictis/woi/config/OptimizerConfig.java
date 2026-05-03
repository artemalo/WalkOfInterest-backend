package sfedu.ictis.woi.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация отбора маршрута.
 *
 * SpEL-формула:
 *   #corridor   - [0, 1], близость к коридору A↔B
 *   #rating     - [0, 1], нормализованный рейтинг
 *   #status     - 1 для APPROVED, иначе approvedPenalty
 *   #userBonus  - 1 для не-user-сгенерированных, иначе userPoiPenalty
 */
@Configuration
@ConfigurationProperties(prefix = "app.optimizer")
@Data
public class OptimizerConfig {
    /**
     * Жёсткий потолок числа POI в итоговом маршруте
     */
    private int maxTotalPois = 50;

    /**
     * Множитель к maxTime для жёсткого constraint'а
     */
    private double hardTimeFactor = 1.05;

    /**
     * Целевое время = maxTime * softTimeFactor
     */
    private double softTimeFactor = 0.95;

    /**
     * для corridor_score
     */
    private double corridorSigmaMeters = 300.0;

    /** Множитель безопасности для эллипса pre-фильтра. */
    private double ellipseSafetyFactor = 1.10;

    /**
     * Бонус за разнообразие категорий
     */
    private double diversityBonus = 0.05;

    /**
     * Финальная формула score POI
     */
    private String poiFormula =
            "(#corridor * 0.55 + #rating * 0.30) * #status * #userBonus";

    private Defaults defaults = new Defaults();
    private Weights weights = new Weights();

    @Data
    public static class Defaults {
        private double rate = 3.0;
        private int count = 0;
    }

    @Data
    public static class Weights {
        private double approvedPenalty = 0.10;
        private double userPoiPenalty = 0.90;
    }
}
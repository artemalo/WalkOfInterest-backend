package sfedu.ictis.woi.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.optimizer")
@Data
public class OptimizerConfig {
    private int maxCategories = 8;
    private int maxSubcategories = 3;
    private int dropBatchSize = 1;

    private String subcategoryFormula = "(#avgRate * 0.5) + (T(java.lang.Math).log10(#totalCount + 1) * 0.3) + (#poiCount * 0.2)";
    private String poiFormula = "(#rate * 0.6) + (T(java.lang.Math).log10(#countRate + 1) * 0.2) + (#distWeight * 0.2) + (#userPoiBonus * 0.3)";

    private Defaults defaults = new Defaults();

    @Data
    public static class Defaults {
        private double rate = 3.5;
        private int count = 2;
    }
}
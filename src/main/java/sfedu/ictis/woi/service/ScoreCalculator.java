package sfedu.ictis.woi.service;

import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;
import sfedu.ictis.woi.config.OptimizerConfig;
import sfedu.ictis.woi.model.dto.PoiDTO;
import sfedu.ictis.woi.model.dto.SubCategoryDTO;

@Service
public class ScoreCalculator {

    private final OptimizerConfig config;
    private final ExpressionParser parser = new SpelExpressionParser();

    public ScoreCalculator(OptimizerConfig config) {
        this.config = config;
    }

    /**
     * Считаем баллы для POI
     */
    public double calculatePoiScore(PoiDTO poi, double distanceWeight) {
        StandardEvaluationContext context = new StandardEvaluationContext();

        // Если данных нет, берем дефолты из конфига
        double rate = (poi.getRate() == null || poi.getRate() == 0) ? config.getDefaults().getRate() : poi.getRate();
        int count = (poi.getCount() == null || poi.getCount() == 0) ? config.getDefaults().getCount() : poi.getCount();

        context.setVariable("rate", rate);
        context.setVariable("countRate", count);
        context.setVariable("distWeight", distanceWeight);
        // (is_not_userPoi)
        context.setVariable("userPoiBonus", 1.0);

        Double result = parser.parseExpression(config.getPoiFormula())
                .getValue(context, Double.class);

        if (result == null) {
            throw new IllegalStateException("POI formula returned null: " + config.getPoiFormula());
        }

        return result;
    }

    /**
     * Считаем баллы для подкатегории (на основе агрегации её точек)
     */
    public double calculateSubcategoryScore(SubCategoryDTO sub) {
        if (sub.getPois().isEmpty()) return 0.0;

        StandardEvaluationContext context = new StandardEvaluationContext();

        double avgRate = sub.getPois().stream()
                .mapToDouble(p -> p.getRate() != null ? p.getRate() : config.getDefaults().getRate())
                .average().orElse(0.0);

        int totalCount = sub.getPois().stream()
                .mapToInt(p -> p.getCount() != null ? p.getCount() : config.getDefaults().getCount())
                .sum();

        context.setVariable("avgRate", avgRate);
        context.setVariable("totalCount", totalCount);
        context.setVariable("poiCount", sub.getPois().size());

        Double result = parser.parseExpression(config.getSubcategoryFormula()).getValue(context, Double.class);

        if (result == null) {
            throw new IllegalStateException("POI formula returned null: " + config.getSubcategoryFormula());
        }

        return result;
    }
}
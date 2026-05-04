package sfedu.ictis.woi.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;
import sfedu.ictis.woi.config.OptimizerConfig;
import sfedu.ictis.woi.model.dto.PoiDTO;

/**
 * Считает score одного POI для отбора маршрута
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScoreCalculatorService {

    private final OptimizerConfig config;
    private final ExpressionParser parser = new SpelExpressionParser();

    private Expression cachedExpression;
    private String cachedFormula;

    /**
     * @param poi             точка интереса
     * @param corridorScore   [0..1]: 1 - если прямо на прямой A-B, 0 - очень далеко
     * @return неотрицательный score; чем больше, тем приоритетнее
     */
    public double calculate(PoiDTO poi, double corridorScore) {
        StandardEvaluationContext ctx = new StandardEvaluationContext();

        double ratingScore = getRatingScore(poi);
        double interestScore = getInterestScore(poi);

        boolean approved = poi.getStatus() != null && "APPROVED".equals(poi.getStatus().name());
        double statusMul = approved ? 1.0 : config.getWeights().getApprovedPenalty();
        double userMul   = poi.isUserGenerated() ? config.getWeights().getUserPoiPenalty() : 1.0;

        ctx.setVariable("corridor", clamp01(corridorScore));
        ctx.setVariable("interest", clamp01(interestScore));
        ctx.setVariable("rating", ratingScore);
        ctx.setVariable("status", statusMul);
        ctx.setVariable("userBonus", userMul);

        Double result = getExpression().getValue(ctx, Double.class);
        if (result == null || result.isNaN() || result.isInfinite()) {
            log.warn("Bad SpEL formula '{}' returned {} for POI id={}",
                    config.getPoiFormula(), result, poi.getId());
            return 0.0;
        }
        return Math.max(0.0, result);
    }

    private double getRatingScore(PoiDTO poi) {
        double rate = (poi.getRate() == null || poi.getRate() == 0)
                ? config.getDefaults().getRate()
                : poi.getRate();
        int count = (poi.getCount() == null) ? config.getDefaults().getCount() : poi.getCount();

        // rating_score: sigmoid от ((rate−3) × log10(count+1))
        return sigmoid((rate - 3.0) * Math.log10(count + 1.0));
    }

    private double getInterestScore(PoiDTO poi) {
        if (poi.getInterest() == null) {
            return config.getDefaults().getInterest();
        }
        return poi.getInterest();
    }

    private Expression getExpression() {
        String formula = config.getPoiFormula();
        if (cachedExpression == null || !formula.equals(cachedFormula)) {
            cachedExpression = parser.parseExpression(formula);
            cachedFormula = formula;
        }
        return cachedExpression;
    }

    private static double sigmoid(double x) {
        return 1.0 / (1.0 + Math.exp(-x));
    }

    private static double clamp01(double v) {
        if (v < 0.0) return 0.0;
        return Math.min(v, 1.0);
    }
}
package sfedu.ictis.woi.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import sfedu.ictis.woi.config.OptimizerConfig;
import sfedu.ictis.woi.exception.BusinessException;
import sfedu.ictis.woi.infrastructure.client.GraphHopperClient;
import sfedu.ictis.woi.model.SearchResponse;
import sfedu.ictis.woi.model.dto.CategoryDTO;
import sfedu.ictis.woi.model.dto.PoiDTO;
import sfedu.ictis.woi.model.dto.PointDTO;
import sfedu.ictis.woi.model.dto.SearchRequestDTO;
import sfedu.ictis.woi.model.dto.SubCategoryDTO;
import sfedu.ictis.woi.service.selection.Candidate;
import sfedu.ictis.woi.service.selection.GeoUtils;
import sfedu.ictis.woi.service.selection.RouteAssembler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Отбор и ранжирование POI для маршрута между двумя точками с ограничением по времени
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OptimizationService {
    private final OptimizerConfig config;
    private final ScoreCalculatorService scoreCalculator;
    private final GraphHopperClient ghClient;

    public void optimize(SearchResponse response, SearchRequestDTO request) {
        if (response == null || response.getCategories() == null || response.getCategories().isEmpty()) {
            log.debug("optimize: empty response, nothing to do");
            return;
        }
        if (request.getMaxTime() == null || request.getMaxTime() <= 0) {
            log.warn("optimize: invalid maxTime={}, clearing categories", request.getMaxTime());
            response.setCategories(List.of());
            return;
        }

        long startTs = System.currentTimeMillis();

        PointDTO p1 = request.getP1();
        PointDTO p2 = request.getP2();
        int maxTime = request.getMaxTime();
        double hardBudget = maxTime * config.getHardTimeFactor();

        double directWalkMin = ghClient.calculateRouteTime(List.of(p1, p2)); // GeoUtils.walkMinutes(GeoUtils.haversine(p1, p2))
        double minAcceptable = directWalkMin * config.getMinTimeFactor();

        if (maxTime < minAcceptable) {
            log.warn("optimize: maxTime={}m < minAcceptable={}m (directWalk={}m). Reject.",
                    maxTime, (int) Math.ceil(minAcceptable), (int) Math.ceil(directWalkMin));
            throw new BusinessException(
                    "maxTime слишком мал " +
                            "(минимум: " + (int) Math.ceil(minAcceptable) + "м; " +
                            "прямая дорога ~" + (int) Math.ceil(directWalkMin) + "м)"
            );
        }

        List<Candidate> all = flatten(response);
        log.info("optimize: total POIs in response = {}", all.size());

        all.forEach(c -> c.poi().setSelected(false));

        double majorAxisM = (maxTime * GeoUtils.WALK_SPEED_M_PER_MIN) * config.getEllipseSafetyFactor();
        List<Candidate> inEllipse = all.stream()
                .filter(c -> GeoUtils.insideEllipse(
                        c.lat(), c.lon(),
                        p1.getLat(), p1.getLon(),
                        p2.getLat(), p2.getLon(),
                        majorAxisM))
                .collect(Collectors.toList());
        log.info("optimize: after ellipse filter = {} (majorAxis={}m)", inEllipse.size(), (int) majorAxisM);

        if (inEllipse.isEmpty()) {
            response.setCategories(List.of());
            return;
        }

        scoreCandidates(inEllipse, p1, p2);

        applyDiversityBonus(inEllipse);

        inEllipse.sort(Comparator.comparingDouble(Candidate::score).reversed());

        RouteAssembler assembler = new RouteAssembler(p1, p2, hardBudget, config.getMaxTotalPois());
        List<Candidate> picked = assembler.assemble(inEllipse);
        log.info("optimize: after assembler = {} POIs (hardBudget={}min)", picked.size(), (int) hardBudget);

        picked = validateAndTrim(picked, p1, p2, hardBudget);
        log.info("optimize: after GH validation = {} POIs", picked.size());

        applySelection(response, picked, p1, p2);

        log.info("optimize: done in {}ms, picked={}/{}",
                System.currentTimeMillis() - startTs, picked.size(), all.size());
    }



    private List<Candidate> flatten(SearchResponse response) {
        List<Candidate> list = new ArrayList<>();
        for (CategoryDTO cat : response.getCategories()) {
            if (cat.getSubcategories() == null) continue;
            for (SubCategoryDTO sub : cat.getSubcategories()) {
                if (sub.getPois() == null) continue;
                for (PoiDTO poi : sub.getPois()) {
                    if (poi.getLat() == null || poi.getLon() == null) continue;
                    list.add(new Candidate(poi, sub, cat, 0.0));
                }
            }
        }
        return list;
    }

    private void scoreCandidates(List<Candidate> candidates, PointDTO p1, PointDTO p2) {
        double sigma = config.getCorridorSigmaMeters();
        for (Candidate c : candidates) {
            double detour = GeoUtils.distanceToSegment(
                    c.lat(), c.lon(),
                    p1.getLat(), p1.getLon(),
                    p2.getLat(), p2.getLon()
            );
            // corridor_score = exp(−detour²/(2σ²)) - нормальное спадание | (0, 1]
            double corridor = Math.exp(-(detour * detour) / (2.0 * sigma * sigma));
            double base = scoreCalculator.calculate(c.poi(), corridor);
            c.setScore(base);
        }
    }

    private void applyDiversityBonus(List<Candidate> candidates) {
        Map<Integer, Candidate> bestPerCategory = new LinkedHashMap<>();
        for (Candidate c : candidates) {
            Integer catId = c.cat().getId();
            if (catId == null) continue;
            Candidate cur = bestPerCategory.get(catId);
            if (cur == null || c.score() > cur.score()) {
                bestPerCategory.put(catId, c);
            }
        }
        double bonus = config.getDiversityBonus();
        for (Candidate champ : bestPerCategory.values()) {
            champ.setScore(champ.score() + bonus);
        }
    }

    /**
     * haversine соврал - выкидываем худшие POI по метрике score/time_share, пока не уложимся
     */
    private List<Candidate> validateAndTrim(List<Candidate> picked, PointDTO p1, PointDTO p2, double hardBudget) {
        if (picked.isEmpty()) return picked;

        double realMinutes;
        try {
            realMinutes = ghClient.calculateRouteTime(buildPoints(p1, picked, p2));
        } catch (Exception e) {
            log.warn("GH validation failed, trusting haversine estimate: {}", e.getMessage());
            return picked;
        }

        if (realMinutes <= hardBudget) {
            return picked;
        }

        log.info("optimize: GH says {}min > hard {}min, trimming...", (int) realMinutes, (int) hardBudget);

        List<Candidate> mutable = new ArrayList<>(picked);

        double initialHaversine = haversineRouteMinutes(p1, mutable, p2);
        if (initialHaversine <= 0) return mutable;

        double ghToHavRatio = realMinutes / initialHaversine;

        while (!mutable.isEmpty()) {
            double currentHav = haversineRouteMinutes(p1, mutable, p2);
            double estimatedReal = currentHav * ghToHavRatio;
            if (estimatedReal <= hardBudget) break;

            int worstIdx = -1;
            double worstRatio = Double.MAX_VALUE;

            for (int i = 0; i < mutable.size(); i++) {
                double saved = removalSavedMinutes(mutable, i, p1, p2);
                if (saved <= 1e-6) continue;
                double r = mutable.get(i).score() / saved;
                if (r < worstRatio) {
                    worstRatio = r;
                    worstIdx = i;
                }
            }
            if (worstIdx < 0) break;
            mutable.remove(worstIdx);
        }

        if (!mutable.isEmpty()) {
            try {
                double check = ghClient.calculateRouteTime(buildPoints(p1, mutable, p2));
                log.info("optimize: GH after trim = {}min (target hard={}min)", (int) check, (int) hardBudget);

                if (check > hardBudget) {
                    double newRatio = check / Math.max(1e-6, haversineRouteMinutes(p1, mutable, p2));
                    while (!mutable.isEmpty()) {
                        double currentHav = haversineRouteMinutes(p1, mutable, p2);
                        if (currentHav * newRatio <= hardBudget) break;
                        int worstIdx = pickWorstByScorePerMinute(mutable, p1, p2);
                        if (worstIdx < 0) break;
                        mutable.remove(worstIdx);
                    }
                }
            } catch (Exception e) {
                log.warn("Final GH check failed: {}", e.getMessage());
            }
        }

        return mutable;
    }

    private int pickWorstByScorePerMinute(List<Candidate> route, PointDTO p1, PointDTO p2) {
        int worstIdx = -1;
        double worstRatio = Double.MAX_VALUE;
        for (int i = 0; i < route.size(); i++) {
            double saved = removalSavedMinutes(route, i, p1, p2);
            if (saved <= 1e-6) continue;
            double r = route.get(i).score() / saved;
            if (r < worstRatio) { worstRatio = r; worstIdx = i; }
        }
        return worstIdx;
    }

    private double removalSavedMinutes(List<Candidate> route, int idx, PointDTO p1, PointDTO p2) {
        int n = route.size();
        double leftLat  = (idx == 0)     ? p1.getLat() : route.get(idx - 1).lat();
        double leftLon  = (idx == 0)     ? p1.getLon() : route.get(idx - 1).lon();
        double rightLat = (idx == n - 1) ? p2.getLat() : route.get(idx + 1).lat();
        double rightLon = (idx == n - 1) ? p2.getLon() : route.get(idx + 1).lon();

        Candidate c = route.get(idx);
        double oldEdges = GeoUtils.walkMinutes(GeoUtils.haversine(leftLat, leftLon, c.lat(), c.lon()))
                + GeoUtils.walkMinutes(GeoUtils.haversine(c.lat(), c.lon(), rightLat, rightLon));
        double newEdge  = GeoUtils.walkMinutes(GeoUtils.haversine(leftLat, leftLon, rightLat, rightLon));
        return oldEdges - newEdge;
    }

    private double haversineRouteMinutes(PointDTO p1, List<Candidate> route, PointDTO p2) {
        if (route.isEmpty()) {
            return GeoUtils.walkMinutes(GeoUtils.haversine(p1, p2));
        }
        double total = GeoUtils.walkMinutes(GeoUtils.haversine(
                p1.getLat(), p1.getLon(), route.getFirst().lat(), route.getFirst().lon()));
        for (int i = 1; i < route.size(); i++) {
            total += GeoUtils.walkMinutes(GeoUtils.haversine(
                    route.get(i - 1).lat(), route.get(i - 1).lon(),
                    route.get(i).lat(),     route.get(i).lon()));
        }
        total += GeoUtils.walkMinutes(GeoUtils.haversine(
                route.getLast().lat(), route.getLast().lon(),
                p2.getLat(), p2.getLon()));
        return total;
    }

    private List<PointDTO> buildPoints(PointDTO p1, List<Candidate> route, PointDTO p2) {
        List<PointDTO> pts = new ArrayList<>(route.size() + 2);
        pts.add(p1);
        for (Candidate c : route) {
            pts.add(new PointDTO(c.lat(), c.lon()));
        }
        pts.add(p2);
        return pts;
    }

    private void applySelection(SearchResponse response, List<Candidate> picked, PointDTO p1, PointDTO p2) {
        Set<Long> pickedIds = new HashSet<>();
        Map<Integer, Integer> selectedPerCategory = new LinkedHashMap<>();
        Map<Integer, Double>  timePerCategory     = new LinkedHashMap<>();
        Set<Integer> usedCategoryIds = new HashSet<>();

        for (Candidate c : picked) {
            pickedIds.add(c.id());
            usedCategoryIds.add(c.cat().getId());
            selectedPerCategory.merge(c.cat().getId(), 1, Integer::sum);
        }

        if (!picked.isEmpty()) {
            double e0 = GeoUtils.walkMinutes(GeoUtils.haversine(
                    p1.getLat(), p1.getLon(), picked.getFirst().lat(), picked.getFirst().lon()));
            timePerCategory.merge(picked.getFirst().cat().getId(), e0, Double::sum);

            for (int i = 1; i < picked.size(); i++) {
                double e = GeoUtils.walkMinutes(GeoUtils.haversine(
                        picked.get(i - 1).lat(), picked.get(i - 1).lon(),
                        picked.get(i).lat(),     picked.get(i).lon()));
                timePerCategory.merge(picked.get(i).cat().getId(), e, Double::sum);
            }

            double last = GeoUtils.walkMinutes(GeoUtils.haversine(
                    picked.getLast().lat(), picked.getLast().lon(),
                    p2.getLat(), p2.getLon()));
            timePerCategory.merge(picked.getLast().cat().getId(), last, Double::sum);
        }

        // оставляем POI только в первой подкатегории, где он встретился
        Set<Long> seenInRendering = new HashSet<>();

        List<CategoryDTO> resultCats = new ArrayList<>();
        for (CategoryDTO cat : response.getCategories()) {
            if (!usedCategoryIds.contains(cat.getId())) continue;

            int totalInCat = 0;
            List<SubCategoryDTO> resultSubs = new ArrayList<>();

            if (cat.getSubcategories() != null) {
                for (SubCategoryDTO sub : cat.getSubcategories()) {
                    if (sub.getPois() == null) continue;

                    List<PoiDTO> subResultPois = new ArrayList<>();
                    for (PoiDTO poi : sub.getPois()) {
                        if (poi.getId() == null) continue;
                        boolean isSelected = pickedIds.contains(poi.getId());
                        if (!isSelected) continue;
                        if (!seenInRendering.add(poi.getId())) continue;

                        poi.setSelected(true);
                        subResultPois.add(poi);
                        totalInCat++;
                    }

                    if (!subResultPois.isEmpty()) {
                        sub.setPois(subResultPois);
                        resultSubs.add(sub);
                    }
                }
            }

            if (resultSubs.isEmpty()) continue;

            cat.setSubcategories(resultSubs);
            cat.setSelected(selectedPerCategory.getOrDefault(cat.getId(), 0));
            cat.setTotalPois(totalInCat);
            cat.setTime((int) Math.round(timePerCategory.getOrDefault(cat.getId(), 0.0)));
            resultCats.add(cat);
        }

        response.setCategories(resultCats);
    }
}
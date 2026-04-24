package sfedu.ictis.woi.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import sfedu.ictis.woi.config.OptimizerConfig;
import sfedu.ictis.woi.infrastructure.client.GraphHopperClient;
import sfedu.ictis.woi.model.RouteResponse;
import sfedu.ictis.woi.model.SearchResponse;
import sfedu.ictis.woi.model.dto.*;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OptimizationService {
    private static final double R = 6371000;
    // person >30-40%, чем расстояние по прямой
    private static final double DETOUR_FACTOR = 1.4;
    private static final double WALKING_SPEED_M_PER_MIN = 80.0; // ~4.8 км/ч

    private final OptimizerConfig config;
    private final ScoreCalculatorService scoreCalculatorService;
    private final GraphHopperClient ghClient;

    public void optimize(SearchResponse response, SearchRequestDTO request) {
        RouteResponse baseRoute = ghClient.getFromToRoute(request.getP1(), request.getP2());

        calculateAndSortScores(response, baseRoute.route());

        List<PoiDTO> candidates = selectCandidatesWithLimits(response);

        int bestCount = findBestFitCount(candidates, request.getMaxTime());

        finalizeCategories(response, request, candidates, bestCount);

        pruneResponse(response);
    }

    private void pruneResponse(SearchResponse response) {
        List<CategoryDTO> prunedCategories = response.getCategories().stream()
                .peek(cat -> {
                    List<SubCategoryDTO> prunedSubs = cat.getSubcategories().stream()
                            .peek(sub -> {
                                List<PoiDTO> selectedPois = sub.getPois().stream()
                                        .filter(poi -> Boolean.TRUE.equals(poi.getSelected()))
                                        .collect(Collectors.toList());
                                sub.setPois(selectedPois);
                            })
                            .filter(sub -> !sub.getPois().isEmpty())
                            .collect(Collectors.toList());
                    cat.setSubcategories(prunedSubs);
                })
                .filter(cat -> !cat.getSubcategories().isEmpty())

                .limit(config.getMaxCategories())
                .collect(Collectors.toList());

        response.setCategories(prunedCategories);
    }

    private List<PoiDTO> selectCandidatesWithLimits(SearchResponse response) {
        Map<PoiDTO, CategoryDTO> poiToCategory = new HashMap<>();
        List<PoiDTO> allPois = new ArrayList<>();

        for (CategoryDTO cat : response.getCategories()) {
            for (SubCategoryDTO sub : cat.getSubcategories()) {
                for (PoiDTO poi : sub.getPois()) {
                    poi.setSelected(false);
                    poiToCategory.put(poi, cat);
                    allPois.add(poi);
                }
            }
        }

        allPois.sort(Comparator.comparing(PoiDTO::getScore).reversed());

        List<PoiDTO> candidates = new ArrayList<>();
        Map<CategoryDTO, Integer> categoryCounts = new HashMap<>();
        Set<CategoryDTO> usedCategories = new HashSet<>();

        int maxPoisPerCat = config.getMaxPoisPerCategory();

        for (PoiDTO poi : allPois) {
            CategoryDTO cat = poiToCategory.get(poi);

            if (!usedCategories.contains(cat) && usedCategories.size() >= config.getMaxCategories()) {
                continue;
            }

            int currentCount = categoryCounts.getOrDefault(cat, 0);

            if (currentCount < maxPoisPerCat) {
                poi.setSelected(true);
                candidates.add(poi);
                categoryCounts.put(cat, currentCount + 1);
                usedCategories.add(cat);
            }
        }

        return candidates;
    }

    private int findBestFitCount(List<PoiDTO> selectedPois, int maxT) {
        if (selectedPois.isEmpty()) return 0;

        selectedPois.sort(Comparator.comparing(PoiDTO::getScore).reversed());

        double[][] matrix = calculateLocalMatrix(selectedPois);

        int left = 1;
        int right = selectedPois.size();
        int best = 0;

        while (left <= right) {
            int mid = left + (right - left) / 2;
            double estimatedTime = calculateGreedyRouteTime(matrix, mid);

            if (estimatedTime <= maxT) {
                best = mid;
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }
        return best;
    }

    private void calculateAndSortScores(SearchResponse response, List<PointDTO> baseRoutePoints) {
        for (CategoryDTO cat : response.getCategories()) {
            for (SubCategoryDTO sub : cat.getSubcategories()) {
                for (PoiDTO poi : sub.getPois()) {
                    double distToRoute = minDistanceToRoute(poi, baseRoutePoints);
                    double proximityBonus;
                    if (distToRoute <= 150.0) {
                        proximityBonus = 1000.0;
                    } else {
                        proximityBonus = Math.max(0, 1000.0 / distToRoute);
                    }
                    poi.setScore(scoreCalculatorService.calculatePoiScore(poi, proximityBonus));
                }
                sub.getPois().sort(Comparator.comparing(PoiDTO::getScore).reversed());
                sub.setScore(scoreCalculatorService.calculateSubcategoryScore(sub));
            }
            cat.getSubcategories().sort(Comparator.comparing(SubCategoryDTO::getScore).reversed());
        }
    }

    private void finalizeCategories(SearchResponse response, SearchRequestDTO request,
                                    List<PoiDTO> sortedCandidates, int bestCount) {
        List<PoiDTO> winners = sortedCandidates.subList(0, bestCount);

        for (CategoryDTO cat : response.getCategories()) {
            List<PoiDTO> winnerPoisInCat = cat.getSubcategories().stream()
                    .flatMap(sub -> sub.getPois().stream())
                    .filter(winners::contains)
                    .toList();

            cat.setSelected(winnerPoisInCat.size());

            // TODO: Сбрасываем флаг selected у всех, оставляем true только у победителей
            cat.getSubcategories().forEach(sub ->
                    sub.getPois().forEach(p -> p.setSelected(winners.contains(p)))
            );

            if (!winnerPoisInCat.isEmpty()) {
                List<PointDTO> fullRoute = new ArrayList<>();
                fullRoute.add(request.getP1());
                fullRoute.addAll(winnerPoisInCat.stream()
                        .map(p -> new PointDTO(p.getLat(), p.getLon()))
                        .toList());
                fullRoute.add(request.getP2());

                try {
                    long realTime = ghClient.calculateRouteTime(fullRoute);
                    cat.setTime((int) realTime);
                } catch (Exception e) {
                    cat.setTime(0);
                }
            } else {
                cat.setTime(0);
            }
        }
    }

    private double[][] calculateLocalMatrix(List<PoiDTO> pois) {
        int n = pois.size();
        double[][] matrix = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i == j) {
                    matrix[i][j] = 0;
                } else {
                    double dist = haversine(pois.get(i), pois.get(j));
                    matrix[i][j] = (dist * DETOUR_FACTOR) / WALKING_SPEED_M_PER_MIN;
                }
            }
        }
        return matrix;
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private double haversine(PoiDTO p1, PoiDTO p2) {
        return haversine(p1.getLat(), p1.getLon(), p2.getLat(), p2.getLon());
    }

    private double minDistanceToRoute(PoiDTO poi, List<PointDTO> routePoints) {
        double minDistance = Double.MAX_VALUE;
        for (PointDTO pt : routePoints) {
            double dist = haversine(poi.getLat(), poi.getLon(), pt.lat(), pt.lon());
            if (dist < minDistance) {
                minDistance = dist;
            }
        }
        return minDistance;
    }

    private double calculateGreedyRouteTime(double[][] matrix, int size) {
        if (size <= 1) return 0;
        boolean[] visited = new boolean[size];
        int current = 0;
        visited[0] = true;
        double total = 0;

        for (int i = 1; i < size; i++) {
            int next = -1;
            double min = Double.MAX_VALUE;
            for (int j = 0; j < size; j++) {
                if (!visited[j] && matrix[current][j] < min) {
                    min = matrix[current][j];
                    next = j;
                }
            }
            if (next != -1) {
                visited[next] = true;
                total += min;
                current = next;
            }
        }
        return total;
    }
}
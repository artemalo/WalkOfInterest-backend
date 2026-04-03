package sfedu.ictis.woi.service;

import org.springframework.stereotype.Service;
import sfedu.ictis.woi.config.OptimizerConfig;
import sfedu.ictis.woi.infrastructure.client.GraphHopperClient;
import sfedu.ictis.woi.model.SearchResponse;
import sfedu.ictis.woi.model.dto.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OptimizationService {
    private static final double R = 6371000;
    // person >30-40%, чем расстояние по прямой
    private static final double DETOUR_FACTOR = 1.4;
    private static final double WALKING_SPEED_M_PER_MIN = 80.0; // ~4.8 км/ч

    private final OptimizerConfig config;
    private final ScoreCalculator scoreCalculator;
    private final GraphHopperClient ghClient;

    public OptimizationService(OptimizerConfig config, ScoreCalculator scoreCalculator, GraphHopperClient ghClient) {
        this.config = config;
        this.scoreCalculator = scoreCalculator;
        this.ghClient = ghClient;
    }

    public void optimize(SearchResponse response, SearchRequestDTO request) {
        calculateAndSortScores(response);
        markInitialTopPois(response);

        List<PoiDTO> candidates = getSelectedPois(response);

        fitToTimeLimit(candidates, request.getMaxTime());

        finalizeCategories(response, request);
    }

    private void fitToTimeLimit(List<PoiDTO> selectedPois, int maxT) {
        if (selectedPois.isEmpty()) return;

        selectedPois.sort(Comparator.comparing(PoiDTO::getScore).reversed());

        double[][] matrix = calculateLocalMatrix(selectedPois);

        int left = 1;
        int right = selectedPois.size();
        int best = 0;

        while (left <= right) {
            int mid = left + (right - left) / 2;
            // жадным алгоритмом примерное время
            double estimatedTime = calculateGreedyRouteTime(matrix, mid);

            if (estimatedTime <= maxT) {
                best = mid;
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }

        for (int i = 0; i < selectedPois.size(); i++) {
            selectedPois.get(i).setSelected(i < best ? 1 : 0);
        }
    }

    /**
     * Рассчитывает баллы для каждой точки и сортирует структуру "сверху вниз".
     */
    private void calculateAndSortScores(SearchResponse response) {
        for (CategoryDTO cat : response.getCategories()) {
            for (SubCategoryDTO sub : cat.getSubcategories()) {
                for (PoiDTO poi : sub.getPois()) {
                    // Рассчитываем индивидуальный скор точки
                    poi.setScore(scoreCalculator.calculatePoiScore(poi, 1.0));
                }
                // Сортируем точки в подкатегории: лучшие в начале
                sub.getPois().sort(Comparator.comparing(PoiDTO::getScore).reversed());

                // Считаем скор всей подкатегории (на основе её точек)
                sub.setScore(scoreCalculator.calculateSubcategoryScore(sub));
            }
            // Сортируем подкатегории внутри категории
            cat.getSubcategories().sort(Comparator.comparing(SubCategoryDTO::getScore).reversed());
        }
    }

    /**
     * Помечает топовые точки как "выбранные" исходя из настроек количества категорий.
     */
    private void markInitialTopPois(SearchResponse response) {
        response.getCategories().stream()
                .limit(config.getMaxCategories())
                .forEach(cat -> {
                    cat.getSubcategories().stream()
                            .limit(config.getMaxSubcategories())
                            .forEach(sub -> {
                                if (!sub.getPois().isEmpty()) {
                                    // Помечаем только самую лучшую точку в каждой подкатегории
                                    sub.getPois().getFirst().setSelected(1);
                                }
                            });
                });
    }

    private List<PoiDTO> getSelectedPois(SearchResponse response) {
        return response.getCategories().stream()
                .flatMap(cat -> cat.getSubcategories().stream())
                .flatMap(sub -> sub.getPois().stream())
                .filter(poi -> poi.getSelected() == 1)
                .collect(Collectors.toList());
    }

    private void finalizeCategories(SearchResponse response, SearchRequestDTO request) {
        for (CategoryDTO cat : response.getCategories()) {
            List<PointDTO> activePoints = cat.getSubcategories().stream()
                    .flatMap(sub -> sub.getPois().stream())
                    .filter(p -> p.getSelected() == 1)
                    .map(p -> new PointDTO(p.getLat(), p.getLon()))
                    .toList();

            if (!activePoints.isEmpty()) {
                cat.setSelected(1);

                List<PointDTO> fullRoute = new ArrayList<>();
                fullRoute.add(request.getP1());
                fullRoute.addAll(activePoints);
                fullRoute.add(request.getP2());

                try {
                    long realTime = ghClient.calculateRouteTime(fullRoute);
                    cat.setTime((int) realTime);
                } catch (Exception e) {
                    cat.setTime(0);
                }
            } else {
                cat.setSelected(0);
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
                    // Дистанция * коэффициент извилистости / скорость
                    matrix[i][j] = (dist * DETOUR_FACTOR) / WALKING_SPEED_M_PER_MIN;
                }
            }
        }
        return matrix;
    }

    private double haversine(PoiDTO p1, PoiDTO p2) {
        double dLat = Math.toRadians(p2.getLat() - p1.getLat());
        double dLon = Math.toRadians(p2.getLon() - p1.getLon());
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(p1.getLat())) * Math.cos(Math.toRadians(p2.getLat())) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
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
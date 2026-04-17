package sfedu.ictis.woi.service;

import org.springframework.stereotype.Service;
import sfedu.ictis.woi.config.OptimizerConfig;
import sfedu.ictis.woi.infrastructure.client.GraphHopperClient;
import sfedu.ictis.woi.model.RouteResponse;
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
        // 1. Получаем базовый "скелетный" маршрут от А до Б
        RouteResponse baseRoute = ghClient.getRoute(request.getP1(), request.getP2());

        // 2. Считаем скоры, передавая маршрут для расчета бонуса близости
        calculateAndSortScores(response, baseRoute.route());

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
            selectedPois.get(i).setSelected(i < best);
        }
    }

    /**
     * Рассчитывает баллы для каждой точки и сортирует структуру "сверху вниз".
     */
    private void calculateAndSortScores(SearchResponse response, List<PointDTO> baseRoutePoints) {
        for (CategoryDTO cat : response.getCategories()) {
            for (SubCategoryDTO sub : cat.getSubcategories()) {
                for (PoiDTO poi : sub.getPois()) {

                    // Узнаем, как далеко точка от скелета маршрута
                    double distToRoute = minDistanceToRoute(poi, baseRoutePoints);

                    // Превращаем расстояние в бонус.
                    // Если точка ближе 150 метров — даем огромный вес.
                    // Если дальше — даем небольшой бонус, зависящий от расстояния (чем ближе, тем лучше).
                    double proximityBonus;
                    if (distToRoute <= 150.0) {
                        proximityBonus = 1000.0; // 100% гарантия, что она выживет при сортировке
                    } else {
                        // Например, 1000 / дистанцию. Если дистанция 500м, бонус будет 2.0
                        proximityBonus = Math.max(0, 1000.0 / distToRoute);
                    }

                    // Передаем этот бонус как distanceWeight в твой SpEL парсер
                    poi.setScore(scoreCalculator.calculatePoiScore(poi, proximityBonus));
                }

                sub.getPois().sort(Comparator.comparing(PoiDTO::getScore).reversed());
                sub.setScore(scoreCalculator.calculateSubcategoryScore(sub));
            }
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
                                    sub.getPois().getFirst().setSelected(true);
                                }
                            });
                });
    }

    private List<PoiDTO> getSelectedPois(SearchResponse response) {
        return response.getCategories().stream()
                .flatMap(cat -> cat.getSubcategories().stream())
                .flatMap(sub -> sub.getPois().stream())
                .filter(poi -> poi.getSelected() == true)
                .collect(Collectors.toList());
    }

    private void finalizeCategories(SearchResponse response, SearchRequestDTO request) {
        for (CategoryDTO cat : response.getCategories()) {
            List<PointDTO> activePoints = cat.getSubcategories().stream()
                    .flatMap(sub -> sub.getPois().stream())
                    //.filter(p -> Boolean.TRUE.equals(p.getSelected()))
                    .map(p -> new PointDTO(p.getLat(), p.getLon()))
                    .toList();

            cat.setSelected(activePoints.size());

            if (!activePoints.isEmpty()) {
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

    // Перегружаем haversine для удобства
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

    // Ищем кратчайшее расстояние от POI до базового маршрута (метры)
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
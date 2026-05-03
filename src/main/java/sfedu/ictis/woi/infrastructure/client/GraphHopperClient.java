package sfedu.ictis.woi.infrastructure.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import sfedu.ictis.woi.exception.ExternalServiceException;
import sfedu.ictis.woi.model.RouteResponse;
import sfedu.ictis.woi.model.dto.PointDTO;
import sfedu.ictis.woi.model.dto.RouteDTO;
import tools.jackson.databind.JsonNode;

import java.util.*;

@Slf4j
@Component
public class GraphHopperClient implements GraphHopperRequest {
    private static final double AVG_STEP_LENGTH = 0.75;
    private static final int MS_TO_MIN = 60000;
    private static final String SERVICE_NAME = "GraphHopper";

    private static final double MAX_RELATIVE_TIME = 1.5;
    private static final int MAX_ALTERNATIVES = 4; // без главного маршрута
    private static final int LONGEST_EDGES_TO_REROUTE = 3;
    private static final double MIN_EDGE_LENGTH_FOR_ALT_M = 500.0;
    private static final int SWAP_VARIANTS = 2;

    private final WebClient webClient;

    public GraphHopperClient(WebClient.Builder builder, @Value("${gh.url}") String url) {
        this.webClient = builder.baseUrl(url).build();
    }

    @Override
    public String fetchIsochrone(double lat, double lon, int seconds) {
        JsonNode response = handleErrors(webClient.get()
                .uri(uri -> uri.path("/isochrone")
                        .queryParam("point", lat + "," + lon)
                        .queryParam("time_limit", seconds)
                        .queryParam("profile", "foot")
                        .build())
                .retrieve()
                .bodyToMono(JsonNode.class)
        ).block();

        if (response == null || response.isMissingNode()) {
            throw new ExternalServiceException(SERVICE_NAME, "GraphHopper returned an empty response for isochrone");
        }
        return parseToWkt(response);
    }

    @Override
    public RouteResponse getFromToRoute(PointDTO p1, PointDTO p2) {
        return calculateRoute(List.of(p1, p2));
    }

    @Override
    public long calculateRouteTime(List<PointDTO> pois) {
        JsonNode response = executeRouteRequest(pois, true, false, false, 1).block();
        if (response == null || response.path("paths").isEmpty()) {
            throw new ExternalServiceException(SERVICE_NAME, "Failed to calculate multi-point route time");
        }
        return response.path("paths").get(0).path("time").asLong() / MS_TO_MIN;
    }

    public RouteResponse calculateRoute(List<PointDTO> points) {
        JsonNode response = executeRouteRequest(points, true, true, false, 1).block();
        if (response == null || response.path("paths").isEmpty()) {
            throw new ExternalServiceException(SERVICE_NAME, "Не удалось рассчитать оптимальный маршрут");
        }
        JsonNode path = response.path("paths").get(0);
        return new RouteResponse(
                path.path("time").asLong() / MS_TO_MIN,
                path.path("distance").asDouble(),
                mapToPointList(path.path("points").path("coordinates"))
        );
    }

    @Override
    public List<RouteDTO> getRoutes(List<PointDTO> categories) {
        if (categories == null || categories.size() < 2) {
            throw new ExternalServiceException(SERVICE_NAME, "Need at least 2 points for getRoutes");
        }

        JsonNode mainResponse = executeRouteRequest(categories, true, true, false, 1).block();
        if (mainResponse == null || mainResponse.path("paths").isEmpty()) {
            throw new ExternalServiceException(SERVICE_NAME, "Main route failed");
        }

        JsonNode mainPath = mainResponse.path("paths").get(0);
        RouteDTO mainRoute = jsonToRoute(mainPath);

        log.info("getRoutes: main route = {}min, {}m, {} steps",
                mainRoute.minTime(), (int) mainRoute.distance(), mainRoute.steps());

        long maxAcceptableTime = (long) (mainRoute.minTime() * MAX_RELATIVE_TIME);
        List<RouteDTO> alternatives = new ArrayList<>();

        try {
            alternatives.addAll(buildEdgeAlternatives(categories, mainRoute, maxAcceptableTime));
        } catch (Exception e) {
            log.warn("Edge-alternatives failed (non-fatal): {}", e.getMessage());
        }

        if (categories.size() >= 4) {
            try {
                alternatives.addAll(buildSwapAlternatives(categories, maxAcceptableTime));
            } catch (Exception e) {
                log.warn("Swap-alternatives failed (non-fatal): {}", e.getMessage());
            }
        }

        List<RouteDTO> all = new ArrayList<>();
        all.add(mainRoute);
        all.addAll(alternatives);

        List<RouteDTO> deduplicated = deduplicate(all);
        deduplicated.sort(Comparator.comparingLong(RouteDTO::minTime));

        if (deduplicated.size() > MAX_ALTERNATIVES + 1) {
            deduplicated = new ArrayList<>(deduplicated.subList(0, MAX_ALTERNATIVES + 1));
        }

        log.info("getRoutes: returning {} routes (main + {} alts)",
                deduplicated.size(), Math.max(0, deduplicated.size() - 1));
        return deduplicated;
    }

    private List<RouteDTO> buildEdgeAlternatives(List<PointDTO> waypoints,
                                                 RouteDTO mainRoute,
                                                 long maxAcceptableTime) {
        List<EdgeInfo> edges = new ArrayList<>();
        for (int i = 0; i < waypoints.size() - 1; i++) {
            double dist = haversineMeters(waypoints.get(i), waypoints.get(i + 1));
            edges.add(new EdgeInfo(i, dist));
        }

        edges.sort(Comparator.comparingDouble((EdgeInfo e) -> e.lengthMeters).reversed());

        List<EdgeInfo> targetEdges = new ArrayList<>();
        for (EdgeInfo e : edges) {
            if (e.lengthMeters < MIN_EDGE_LENGTH_FOR_ALT_M) break;
            targetEdges.add(e);
            if (targetEdges.size() >= LONGEST_EDGES_TO_REROUTE) break;
        }

        if (targetEdges.isEmpty()) {
            log.debug("buildEdgeAlternatives: no long enough edges (min {}m)", (int) MIN_EDGE_LENGTH_FOR_ALT_M);
            return List.of();
        }

        List<RouteDTO> result = new ArrayList<>();
        for (EdgeInfo edge : targetEdges) {
            try {
                List<RouteDTO> altsForEdge = fetchAlternativesForEdge(waypoints, edge.index);
                for (RouteDTO altFull : altsForEdge) {
                    if (altFull.minTime() <= maxAcceptableTime) {
                        result.add(altFull);
                    } else {
                        log.debug("Edge-alt rejected: time {} > max {}", altFull.minTime(), maxAcceptableTime);
                    }
                }
            } catch (Exception e) {
                log.warn("alt-route for edge {} failed: {}", edge.index, e.getMessage());
            }
        }
        return result;
    }

    private List<RouteDTO> fetchAlternativesForEdge(List<PointDTO> waypoints, int edgeIdx) {
        PointDTO from = waypoints.get(edgeIdx);
        PointDTO to   = waypoints.get(edgeIdx + 1);

        JsonNode response = executeRouteRequest(List.of(from, to), false, true, true, 3).block();
        if (response == null || response.path("paths").isMissingNode()) {
            return List.of();
        }
        JsonNode paths = response.path("paths");
        if (!paths.isArray() || paths.size() <= 1) {
            return List.of();
        }

        List<RouteDTO> result = new ArrayList<>();
        // Первый путь оптимальный
        for (int i = 1; i < paths.size(); i++) {
            JsonNode altPath = paths.get(i);
            List<PointDTO> altGeometry = mapToPointList(altPath.path("points").path("coordinates"));
            if (altGeometry.size() < 3) {
                // короткая альтернатива - разница минимальная
                continue;
            }

            // Точка из середины альтернативной геометрии (лежит на альтернативной улице)
            PointDTO viaPoint = altGeometry.get(altGeometry.size() / 2);

            List<PointDTO> waypointsWithVia = new ArrayList<>(waypoints);
            waypointsWithVia.add(edgeIdx + 1, viaPoint);

            try {
                JsonNode fullResp = executeRouteRequest(waypointsWithVia, false, true, false, 1).block();
                if (fullResp == null || fullResp.path("paths").isEmpty()) continue;
                RouteDTO fullRoute = jsonToRoute(fullResp.path("paths").get(0));
                result.add(fullRoute);
            } catch (Exception e) {
                log.warn("Full-route via alt-point failed: {}", e.getMessage());
            }
        }
        return result;
    }



    private List<RouteDTO> buildSwapAlternatives(List<PointDTO> waypoints, long maxAcceptableTime) {
        List<RouteDTO> result = new ArrayList<>();
        int innerCount = waypoints.size() - 2;
        if (innerCount < 2) return result;

        Random rnd = new Random(waypoints.hashCode());
        Set<String> triedSwaps = new HashSet<>();

        int attempts = 0;
        int maxAttempts = SWAP_VARIANTS * 4;

        while (result.size() < SWAP_VARIANTS && attempts++ < maxAttempts) {
            int i = 1 + rnd.nextInt(innerCount - 1); // 1..innerCount-1
            int j = i + 1;                            // соседняя точка
            String key = i + "-" + j;
            if (!triedSwaps.add(key)) continue;

            List<PointDTO> swapped = new ArrayList<>(waypoints);
            Collections.swap(swapped, i, j);

            try {
                JsonNode resp = executeRouteRequest(swapped, false, true, false, 1).block();
                if (resp == null || resp.path("paths").isEmpty()) continue;
                JsonNode path = resp.path("paths").get(0);
                long time = path.path("time").asLong() / MS_TO_MIN;
                if (time > maxAcceptableTime) {
                    log.debug("Swap variant rejected: time {} > max {}", time, maxAcceptableTime);
                    continue;
                }
                result.add(jsonToRoute(path));
            } catch (Exception e) {
                log.warn("Swap variant request failed: {}", e.getMessage());
            }
        }
        return result;
    }



    private RouteDTO jsonToRoute(JsonNode path) {
        double distance = path.path("distance").asDouble();
        long timeMs = path.path("time").asLong();
        return new RouteDTO(
                timeMs / MS_TO_MIN,
                distance,
                Math.round(distance / AVG_STEP_LENGTH),
                mapToPointList(path.path("points").path("coordinates"))
        );
    }

    private List<RouteDTO> deduplicate(List<RouteDTO> routes) {
        Set<String> seen = new HashSet<>();
        List<RouteDTO> unique = new ArrayList<>(routes.size());
        for (RouteDTO r : routes) {
            String key = r.minTime() + "|" + Math.round(r.distance()) + "|" + r.steps();
            if (seen.add(key)) unique.add(r);
        }
        return unique;
    }

    private double haversineMeters(PointDTO a, PointDTO b) {
        double R = 6_371_000.0;
        double dLat = Math.toRadians(b.getLat() - a.getLat());
        double dLon = Math.toRadians(b.getLon() - a.getLon());
        double rLat1 = Math.toRadians(a.getLat());
        double rLat2 = Math.toRadians(b.getLat());
        double sinDLat = Math.sin(dLat / 2);
        double sinDLon = Math.sin(dLon / 2);
        double aa = sinDLat * sinDLat + Math.cos(rLat1) * Math.cos(rLat2) * sinDLon * sinDLon;
        return 2 * R * Math.atan2(Math.sqrt(aa), Math.sqrt(1 - aa));
    }

    private List<PointDTO> mapToPointList(JsonNode coordsNode) {
        List<PointDTO> points = new ArrayList<>();
        for (JsonNode coord : coordsNode) {
            points.add(new PointDTO(coord.get(1).asDouble(), coord.get(0).asDouble()));
        }
        return points;
    }

    private Mono<JsonNode> executeRouteRequest(List<PointDTO> points,
                                               boolean optimize,
                                               boolean calcPoints,
                                               boolean alternative,
                                               int maxAltPaths) {
        return handleErrors(webClient.get()
                .uri(uriBuilder -> {
                    var uri = uriBuilder.path("/route");
                    for (PointDTO p : points) {
                        uri.queryParam("point", p.getLat() + "," + p.getLon());
                    }
                    uri.queryParam("profile", "foot")
                            .queryParam("optimize", String.valueOf(optimize))
                            .queryParam("calc_points", calcPoints)
                            .queryParam("points_encoded", false);

                    if (alternative) {
                        uri.queryParam("algorithm", "alternative_route")
                                .queryParam("ch.disable", "true")
                                .queryParam("alternative_route.max_paths", maxAltPaths);
                    }
                    return uri.build();
                })
                .retrieve()
                .bodyToMono(JsonNode.class));
    }

    private String parseToWkt(JsonNode response) {
        JsonNode polygons = response.path("polygons");
        if (polygons.isMissingNode() || !polygons.isArray() || polygons.isEmpty()) {
            throw new RuntimeException("GraphHopper response: 'polygons' is missing or empty");
        }
        JsonNode geometry = polygons.get(0).path("geometry");
        String type = geometry.path("type").asStringOpt()
                .orElseThrow(() -> new RuntimeException("Geometry type is missing"));
        if (!"Polygon".equalsIgnoreCase(type)) {
            throw new UnsupportedOperationException("Expected Polygon, but got: " + type);
        }
        JsonNode exteriorRing = geometry.path("coordinates").path(0);
        if (exteriorRing.isMissingNode() || !exteriorRing.isArray()) {
            throw new RuntimeException("Invalid coordinates structure");
        }
        StringBuilder wkt = new StringBuilder("POLYGON((");
        for (int i = 0; i < exteriorRing.size(); i++) {
            JsonNode point = exteriorRing.get(i);
            wkt.append(point.get(0).asDouble()).append(" ").append(point.get(1).asDouble());
            if (i < exteriorRing.size() - 1) wkt.append(", ");
        }
        wkt.append("))");
        return wkt.toString();
    }

    private <T> Mono<T> handleErrors(Mono<T> mono) {
        return mono
                .onErrorMap(WebClientRequestException.class, ex -> {
                    log.error(SERVICE_NAME + " unavailable", ex);
                    throw new ExternalServiceException(SERVICE_NAME, "Недоступен");
                })
                .onErrorMap(WebClientResponseException.class, ex -> {
                    log.error(SERVICE_NAME + " {}", ex.getStatusCode(), ex);
                    throw new ExternalServiceException(SERVICE_NAME, "Ошибка");
                });
    }

    private record EdgeInfo(int index, double lengthMeters) {}
}
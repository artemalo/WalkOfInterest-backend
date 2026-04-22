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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Component
public class GraphHopperClient implements GraphHopperRequest {
    private static final double AVG_STEP_LENGTH = 0.75;

    private static final int MS_TO_MIN = 60000;
    private static final String SERVICE_NAME = "GraphHopper";

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
                .bodyToMono(JsonNode.class) // 4xx или 5xx выбросит исключение
        ).block();

        if (response == null || response.isMissingNode()) {
            throw new ExternalServiceException(SERVICE_NAME, "GraphHopper returned an empty response for isochrone");
        }

        return parseToWkt(response);
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

            double lon = point.get(0).asDouble();
            double lat = point.get(1).asDouble();

            wkt.append(lon).append(" ").append(lat);

            if (i < exteriorRing.size() - 1) {
                wkt.append(", ");
            }
        }

        wkt.append("))");
        return wkt.toString();
    }

    @Override
    public RouteResponse getFromToRoute(PointDTO p1, PointDTO p2) {
        return calculateRoute(List.of(p1, p2));
    }

    @Override
    public long calculateRouteTime(List<PointDTO> pois) {
        JsonNode response = executeRouteRequest(pois, true, false).block();
        if (response == null || response.path("paths").isEmpty()) {
            throw new ExternalServiceException(SERVICE_NAME, "Failed to calculate multi-point route time");
        }
        return response.path("paths").get(0).path("time").asLong() / MS_TO_MIN;
    }

    @Override
    public List<RouteDTO> getRoutes(List<PointDTO> categories) { //TODO одинаковые не возвращать
        int variantsCount = 5;

        return Flux.range(0, variantsCount)
                .flatMap(i -> {
                    boolean shouldOptimize = (i == 0);
                    List<PointDTO> points = prepareVariant(categories, i);
                    return fetchRouteVariant(points, shouldOptimize)
                            .onErrorResume(e -> {
                                log.error("Ошибка при расчете варианта {}: {}", i, e.getMessage());
                                return Mono.empty(); // TODO throw error
                            });
                })
                .collectList()
                .map(routes -> {
                    routes.sort(Comparator.comparingLong(RouteDTO::minTime));
                    return routes;
                })
                .block();
    }

    private Mono<RouteDTO> fetchRouteVariant(List<PointDTO> points, boolean optimize) {
        return executeRouteRequest(points, optimize, true)
                .map(response -> {
                    JsonNode path = response.path("paths").get(0);
                    double distance = path.path("distance").asDouble();
                    long timeMs = path.path("time").asLong();

                    return new RouteDTO(
                            timeMs / MS_TO_MIN,
                            distance,
                            Math.round(distance / AVG_STEP_LENGTH),
                            mapToPointList(path.path("points").path("coordinates"))
                    );
                });
    }

    private List<PointDTO> prepareVariant(List<PointDTO> original, int variantIndex) {
        if (original.size() <= 2 || variantIndex == 0) {
            return new ArrayList<>(original);
        }

        List<PointDTO> middle = new ArrayList<>(original.subList(1, original.size() - 1));
        Collections.shuffle(middle);

        List<PointDTO> variant = new ArrayList<>();
        variant.add(original.getFirst());
        variant.addAll(middle);
        variant.add(original.getLast());

        return variant;
    }

    public RouteResponse calculateRoute(List<PointDTO> points) {
        JsonNode response = executeRouteRequest(points, true, true).block();

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

    private List<PointDTO> mapToPointList(JsonNode coordsNode) {
        List<PointDTO> points = new ArrayList<>();
        for (JsonNode coord : coordsNode) {
            points.add(new PointDTO(coord.get(1).asDouble(), coord.get(0).asDouble()));
        }
        return points;
    }

    private Mono<JsonNode> executeRouteRequest(List<PointDTO> points, boolean optimize, boolean calcPoints) {
        return handleErrors(webClient.get()
                .uri(uriBuilder -> {
                    var uri = uriBuilder.path("/route");
                    for (PointDTO p : points) {
                        uri.queryParam("point", p.lat() + "," + p.lon());
                    }
                    return uri.queryParam("profile", "foot")
                            .queryParam("optimize", String.valueOf(optimize))
                            .queryParam("calc_points", calcPoints)
                            .queryParam("points_encoded", false)
                            .build();
                })
                .retrieve()
                .bodyToMono(JsonNode.class));
    }

    private <T> Mono<T> handleErrors(Mono<T> mono) {
        return mono.onErrorMap(WebClientRequestException.class, ex -> {
            log.error(SERVICE_NAME + " Недоступен", ex);
            throw new ExternalServiceException(SERVICE_NAME, "Недоступен");
        })
                .onErrorMap(WebClientResponseException.class, ex -> {
                    log.error(SERVICE_NAME + " Недоступен {}", ex.getStatusCode(), ex);
                            throw new ExternalServiceException(SERVICE_NAME, "Ошибка");
                });
    }
}
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
import sfedu.ictis.woi.model.RouteFromToResponse;
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

    @Override
    public RouteFromToResponse getFromToRoute(PointDTO p1, PointDTO p2) {
        JsonNode response = handleErrors(webClient.get()
                .uri(uri -> uri.path("/route")
                        .queryParam("point", p1.lat() + "," + p1.lon())
                        .queryParam("point", p2.lat() + "," + p2.lon())
                        .queryParam("profile", "foot")
                        .queryParam("calc_points", true)
                        .queryParam("points_encoded", false)
                        .build())
                .retrieve()
                .bodyToMono(JsonNode.class)
        ).block();

        if (response == null || response.path("paths").isEmpty()) {
            throw new ExternalServiceException(SERVICE_NAME, "Could not calculate route between points");
        }

        JsonNode path = response.path("paths").get(0);

        long timeMs = path.path("time").asLong();
        double distance = path.path("distance").asDouble();

        List<PointDTO> points = new ArrayList<>();
        JsonNode coords = path.path("points").path("coordinates");

        for (JsonNode coord : coords) {
            double lon = coord.get(0).asDouble();
            double lat = coord.get(1).asDouble();
            points.add(new PointDTO(lat, lon));
        }

        return new RouteFromToResponse(timeMs / MS_TO_MIN, distance, points);
    }

    @Override
    public long calculateRouteTime(List<PointDTO> pois) {
        JsonNode response = handleErrors(
                webClient.get()
                        .uri(uriBuilder -> {
                            var uri = uriBuilder.path("/route");

                            for (PointDTO poi : pois) {
                                uri.queryParam("point", poi.lat() + "," + poi.lon());
                            }

                            return uri
                                    .queryParam("profile", "foot")
                                    .queryParam("optimize", "true")
                                    .queryParam("calc_points", false)
                                    .build();
                        })
                        .retrieve()
                        .bodyToMono(JsonNode.class)
        ).block();

        if (response == null || response.path("paths").isEmpty()) {
            throw new ExternalServiceException(SERVICE_NAME, "Failed to calculate multi-point route");
        }

        long timeMs = response.path("paths").get(0).path("time").asLong();

        return timeMs / MS_TO_MIN;
    }

    @Override
    public List<RouteDTO> getRoutes(List<PointDTO> categories) {
        if (categories == null || categories.size() < 2) {
            return List.of();
        }

        int variantsCount = 5;

        return Flux.range(0, variantsCount)
                .flatMap(i -> {
                    List<PointDTO> variantPoints = prepareVariant(categories, i);
                    return fetchRouteVariant(variantPoints)
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

    private Mono<RouteDTO> fetchRouteVariant(List<PointDTO> points) {
        return webClient.get()
                .uri(uriBuilder -> {
                    var uri = uriBuilder.path("/route");
                    for (PointDTO p : points) {
                        uri.queryParam("point", p.lat() + "," + p.lon());
                    }
                    return uri.queryParam("profile", "foot")
                            .queryParam("optimize", "false")
                            .queryParam("calc_points", true)
                            .queryParam("points_encoded", false)
                            .build();
                })
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(response -> {
                    JsonNode path = response.path("paths").get(0);

                    long timeMs = path.path("time").asLong();
                    double distance = path.path("distance").asDouble();
                    long steps = Math.round(distance / AVG_STEP_LENGTH);

                    List<PointDTO> routePoints = new ArrayList<>();
                    JsonNode coords = path.path("points").path("coordinates");
                    for (JsonNode coord : coords) {
                        routePoints.add(new PointDTO(coord.get(1).asDouble(), coord.get(0).asDouble()));
                    }

                    return new RouteDTO(timeMs / MS_TO_MIN, distance, steps, routePoints);
                });
    }



    private List<PointDTO> prepareVariant(List<PointDTO> original, int variantIndex) {
        if (original.size() <= 2 || variantIndex == 0) {
            return new ArrayList<>(original);
        }

//        if (variantIndex == 0) {
//            return calculateRoute(original);
//        }

        List<PointDTO> middle = new ArrayList<>(original.subList(1, original.size() - 1));
        Collections.shuffle(middle);

        List<PointDTO> variant = new ArrayList<>();
        variant.add(original.getFirst());
        variant.addAll(middle);
        variant.add(original.getLast());

        return variant;
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
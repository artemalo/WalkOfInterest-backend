package sfedu.ictis.woi.infrastructure.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import sfedu.ictis.woi.exception.ExternalServiceException;
import sfedu.ictis.woi.model.dto.PointDTO;
import tools.jackson.databind.JsonNode;

@Component
public class GraphHopperClient {
    private final WebClient webClient;

    public GraphHopperClient(WebClient.Builder builder, @Value("${gh.url}") String url) {
        this.webClient = builder.baseUrl(url).build();
    }

    public String fetchIsochrone(double lat, double lon, int seconds) {
        JsonNode response = webClient.get()
                .uri(uri -> uri.path("/isochrone")
                        .queryParam("point", lat + "," + lon)
                        .queryParam("time_limit", seconds)
                        .queryParam("profile", "foot")
                        .build())
                .retrieve()
                .bodyToMono(JsonNode.class) // 4xx или 5xx выбросит исключение
                .block();

        if (response == null || response.isMissingNode()) {
            throw new ExternalServiceException("GraphHopper returned an empty response for isochrone");
        }

        return parseToWkt(response);
    }

    public long getMinRouteTime(PointDTO p1, PointDTO p2) {
        JsonNode response = webClient.get()
                .uri(uri -> uri.path("/route")
                        .queryParam("point", p1.lat() + "," + p1.lon())
                        .queryParam("point", p2.lat() + "," + p2.lon())
                        .queryParam("profile", "foot")
                        .queryParam("calc_points", false) // только время
                        .queryParam("points_encoded", false)
                        .build())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        if (response == null || response.path("paths").isEmpty()) {
            throw new ExternalServiceException("Could not calculate route between points");
        }

        long timeMs = response.path("paths").get(0).path("time").asLong();

        return timeMs / 1000;
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
}
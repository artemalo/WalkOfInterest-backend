package sfedu.ictis.woi.infrastructure.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import sfedu.ictis.woi.model.RouteResponse;
import sfedu.ictis.woi.model.dto.PointDTO;
import sfedu.ictis.woi.model.dto.RouteDTO;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class GraphHopperCustom implements GraphHopperRequest {
    private static final double EARTH_RADIUS = 6371000;

    @Override
    public String fetchIsochrone(double lat, double lon, int seconds) {
        double speed = 1.4;
        double radius = speed * seconds;

        int points = 64;

        List<double[]> coords = new ArrayList<>();

        for (int i = 0; i <= points; i++) {
            double angle = 2 * Math.PI * i / points;

            double newLat = Math.asin(
                    Math.sin(Math.toRadians(lat)) * Math.cos(radius / EARTH_RADIUS) +
                            Math.cos(Math.toRadians(lat)) * Math.sin(radius / EARTH_RADIUS) * Math.cos(angle)
            );

            double newLon = Math.toRadians(lon) + Math.atan2(
                    Math.sin(angle) * Math.sin(radius / EARTH_RADIUS) * Math.cos(Math.toRadians(lat)),
                    Math.cos(radius / EARTH_RADIUS) - Math.sin(Math.toRadians(lat)) * Math.sin(newLat)
            );

            coords.add(new double[]{
                    Math.toDegrees(newLon),
                    Math.toDegrees(newLat)
            });
        }

        return toWkt(coords);
    }

    @Override
    public RouteResponse getFromToRoute(PointDTO p1, PointDTO p2) {
        log.warn(this.getClass().getSimpleName(), "getFromToRoute");
        return new RouteResponse(-1, -1, new ArrayList<>());
    }

    @Override
    public long calculateRouteTime(List<PointDTO> pois) {
        log.warn(this.getClass().getSimpleName(), "calculateRouteTime");
        return -1;
    }

    @Override
    public List<RouteDTO> getRoutes(List<PointDTO> categories) {
        log.warn(this.getClass().getSimpleName(), "getRoutes");
        return List.of();
    }

    private String toWkt(List<double[]> coords) {
        StringBuilder wkt = new StringBuilder("POLYGON((");

        for (int i = 0; i < coords.size(); i++) {
            double[] p = coords.get(i);

            wkt.append(p[0]).append(" ").append(p[1]);

            if (i < coords.size() - 1) {
                wkt.append(", ");
            }
        }

        wkt.append("))");
        return wkt.toString();
    }
}

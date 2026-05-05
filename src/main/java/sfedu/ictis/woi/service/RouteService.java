package sfedu.ictis.woi.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import sfedu.ictis.woi.exception.BusinessException;
import sfedu.ictis.woi.infrastructure.client.GraphHopperClient;
import sfedu.ictis.woi.model.dto.PointDTO;
import sfedu.ictis.woi.model.dto.RouteDTO;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class RouteService {
    private final GraphHopperClient ghClient;

    public List<RouteDTO> getRoutes(List<PointDTO> categories) {
        if (categories == null || categories.size() < 2) {
            throw new IllegalStateException("Invalid categories");
        }

        // фронт
        log.info("RouteService.getRoutes: input has {} points", categories.size());
        for (int i = 0; i < categories.size(); i++) {
            PointDTO p = categories.get(i);
            log.info("  [{}] lat={}, lon={}", i, p.getLat(), p.getLon());
        }

        // расстояния между соседями по haversine
        double totalKm = 0;
        for (int i = 0; i < categories.size() - 1; i++) {
            double km = haversineKm(categories.get(i), categories.get(i + 1));
            totalKm += km;
            if (km > 5.0) {
                log.warn("  edge [{}->{}] is {}km - подозрительно большое расстояние", i, i + 1, String.format("%.2f", km));
            }
        }
        log.info("RouteService.getRoutes: total straight-line distance = {}km, walk-time estimate ≈ {}min",
                String.format("%.2f", totalKm),
                (int) (totalKm * 1000 / 80));

        try {
            List<RouteDTO> routes = ghClient.getRoutes(categories);

            if (routes.isEmpty()) {
                throw new IllegalStateException("Routes are empty");
            }

            return deduplicate(routes);

        } catch (BusinessException | IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("GraphHopper failed, fallback triggered");
        }
    }

    private static double haversineKm(PointDTO a, PointDTO b) {
        double R = 6371.0;
        double dLat = Math.toRadians(b.getLat() - a.getLat());
        double dLon = Math.toRadians(b.getLon() - a.getLon());
        double rLat1 = Math.toRadians(a.getLat());
        double rLat2 = Math.toRadians(b.getLat());
        double sinDLat = Math.sin(dLat / 2);
        double sinDLon = Math.sin(dLon / 2);
        double aa = sinDLat * sinDLat + Math.cos(rLat1) * Math.cos(rLat2) * sinDLon * sinDLon;
        return 2 * R * Math.atan2(Math.sqrt(aa), Math.sqrt(1 - aa));
    }

    private List<RouteDTO> deduplicate(List<RouteDTO> routes) {
        Set<String> seen = new HashSet<>();
        List<RouteDTO> unique = new ArrayList<>(routes.size());

        for (RouteDTO route : routes) {
            String key = route.minTime() + "|"
                    + Math.round(route.distance())  + "|"
                    + route.steps();

            if (seen.add(key)) {
                unique.add(route);
            }
        }

        return unique;
    }
}
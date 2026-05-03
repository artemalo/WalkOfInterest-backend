package sfedu.ictis.woi.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import sfedu.ictis.woi.exception.BusinessException;
import sfedu.ictis.woi.infrastructure.client.GraphHopperClient;
import sfedu.ictis.woi.model.dto.PointDTO;
import sfedu.ictis.woi.model.dto.RouteDTO;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RouteService {
    private final GraphHopperClient ghClient;

    public List<RouteDTO> getRoutes(List<PointDTO> categories) {
        if (categories == null || categories.size() < 2) {
            throw new IllegalStateException("Invalid categories");
        }

        try {
            List<RouteDTO> routes = ghClient.getRoutes(categories);

            if (routes.isEmpty()) {
                throw new IllegalStateException("Routes are empty");
            }

            return deduplicate(routes);

        } catch (BusinessException | IllegalStateException e) {
            throw e;
        } catch (Exception e) {
//            return fallbackClient.getRoutes;
            throw new BusinessException("GraphHopper failed, fallback triggered");
        }
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
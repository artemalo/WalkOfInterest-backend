package sfedu.ictis.woi.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import sfedu.ictis.woi.exception.BusinessException;
import sfedu.ictis.woi.infrastructure.client.GraphHopperClient;
import sfedu.ictis.woi.model.dto.PointDTO;
import sfedu.ictis.woi.model.dto.RouteDTO;

import java.util.List;

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

            return routes;

        } catch (Exception e) {
//            return fallbackClient.getRoutes;
            throw new BusinessException("GraphHopper failed, fallback triggered");
        }
    }
}

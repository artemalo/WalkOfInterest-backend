package sfedu.ictis.woi.service;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import sfedu.ictis.woi.exception.BusinessException;
import sfedu.ictis.woi.infrastructure.client.GraphHopperClient;
import sfedu.ictis.woi.model.RouteFromToResponse;
import sfedu.ictis.woi.model.dto.PointDTO;


@Slf4j
@Service
public class PoiService {
    private final GraphHopperClient ghClient;

    public PoiService(GraphHopperClient ghClient) {
        this.ghClient = ghClient;
    }

    public RouteFromToResponse getRoute(PointDTO from, PointDTO to) {
        try {
            RouteFromToResponse route = ghClient.getFromToRoute(from, to);

            if (route.minTime() < 0) {
                throw new IllegalStateException("Invalid route time");
            }

            return route;

        } catch (Exception e) {
            log.warn("GraphHopper failed, fallback triggered", e);
//            return fallbackClient.getRoute(from, to);
            throw new BusinessException("GraphHopper failed, fallback triggered");
        }
    }
}

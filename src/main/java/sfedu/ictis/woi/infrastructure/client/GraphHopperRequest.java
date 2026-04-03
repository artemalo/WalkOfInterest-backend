package sfedu.ictis.woi.infrastructure.client;

import sfedu.ictis.woi.model.RouteResponse;
import sfedu.ictis.woi.model.dto.PointDTO;

import java.util.List;

public interface GraphHopperRequest {
    String fetchIsochrone(double lat, double lon, int seconds);
    RouteResponse getRoute(PointDTO p1, PointDTO p2);
    long calculateRouteTime(List<PointDTO> pois);
}

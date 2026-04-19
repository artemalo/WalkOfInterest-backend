package sfedu.ictis.woi.infrastructure.client;

import sfedu.ictis.woi.model.RouteFromToResponse;
import sfedu.ictis.woi.model.dto.PointDTO;
import sfedu.ictis.woi.model.dto.RouteDTO;

import java.util.List;

public interface GraphHopperRequest {
    String fetchIsochrone(double lat, double lon, int seconds);
    RouteFromToResponse getFromToRoute(PointDTO p1, PointDTO p2);
    long calculateRouteTime(List<PointDTO> pois);
    List<RouteDTO> getRoutes(List<PointDTO> categories);
}

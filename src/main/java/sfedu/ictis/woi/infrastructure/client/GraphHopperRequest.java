package sfedu.ictis.woi.infrastructure.client;

import sfedu.ictis.woi.model.dto.PointDTO;

public interface GraphHopperRequest {
    String fetchIsochrone(double lat, double lon, int seconds);
    long getMinRouteTime(PointDTO p1, PointDTO p2);
}

package sfedu.ictis.woi.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import sfedu.ictis.woi.infrastructure.client.GraphHopperClient;
import sfedu.ictis.woi.infrastructure.client.GraphHopperCustom;
import sfedu.ictis.woi.mapper.DataMapper;
import sfedu.ictis.woi.model.PoisResponse;
import sfedu.ictis.woi.model.RouteSearchRequest;
import sfedu.ictis.woi.model.dto.CategoryDTO;
import sfedu.ictis.woi.model.dto.PointDTO;
import sfedu.ictis.woi.projection.FlatPoiProjection;
import sfedu.ictis.woi.repository.PoiRepository;

import java.util.List;

@Service
public class PoiService {
    private static final Logger log = LoggerFactory.getLogger(PoiService.class);

    @Autowired
    private GraphHopperCustom fallbackClient;

    private final GraphHopperClient ghClient;
    private final PoiRepository poiRepository;

    public PoiService(GraphHopperClient ghClient, PoiRepository poiRepository) {
        this.ghClient = ghClient;
        this.poiRepository = poiRepository;
    }

    public long calculateMinTime(PointDTO p1, PointDTO p2) {
        long timeSeconds = ghClient.getMinRouteTime(p1, p2);
        return timeSeconds / 60;
    }

    public PoisResponse getStructuredPois(RouteSearchRequest request) {
        double midLat = (request.p1().lat() + request.p2().lat()) / 2;
        double midLon = (request.p1().lon() + request.p2().lon()) / 2;

        // можно (timeLimit - minTime)
        String isochroneWkt = safeFetchIsochrone(midLat, midLon, request.timeLimitMinutes() * 60);

        List<FlatPoiProjection> flatPois = poiRepository.findPoisInIsochrone(
                request.lang() != null ? request.lang() : "ru",
                isochroneWkt
        );

        List<CategoryDTO> structuredData = DataMapper.mapToHierarchy(flatPois);

        return new PoisResponse(request.requestId(), structuredData);
    }

    private String safeFetchIsochrone(double lat, double lon, int seconds) {
        try {
            return ghClient.fetchIsochrone(lat, lon, seconds);
        } catch (Exception e) {
            log.warn("Fallback to radius polygon", e);
            return fallbackClient.fetchIsochrone(lat, lon, seconds);
        }
    }
}

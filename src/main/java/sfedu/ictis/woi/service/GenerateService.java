package sfedu.ictis.woi.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import sfedu.ictis.woi.exception.BusinessException;
import sfedu.ictis.woi.infrastructure.client.GraphHopperClient;
import sfedu.ictis.woi.infrastructure.client.GraphHopperCustom;
import sfedu.ictis.woi.mapper.DataMapper;
import sfedu.ictis.woi.model.RouteResponse;
import sfedu.ictis.woi.model.SearchResponse;
import sfedu.ictis.woi.model.SearchRequest;
import sfedu.ictis.woi.model.dto.CategoryDTO;
import sfedu.ictis.woi.model.dto.PointDTO;
import sfedu.ictis.woi.projection.FlatPoiProjection;
import sfedu.ictis.woi.repository.PoiRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GenerateService {
    private static final Logger log = LoggerFactory.getLogger(GenerateService.class);

    private final PoiRepository poiRepository;
    private final GraphHopperClient ghClient;
    private final GraphHopperCustom fallbackClient;

    public SearchResponse findAllPois(SearchRequest request) {
        if (request.getMaxTime() < 0) {
            throw new IllegalStateException("Invalid time");
        }

        double midLat = (request.getP1().getLat() + request.getP2().getLat()) / 2;
        double midLon = (request.getP1().getLon() + request.getP2().getLon()) / 2;

        String isochroneWkt = safeFetchIsochrone(midLat, midLon, request.getMaxTime() * 60);

        List<FlatPoiProjection> flatPois = poiRepository.findPoisInIsochrone(
                request.getLang() != null ? request.getLang() : "ru",
                isochroneWkt
        );

        log.info("SearchService: point={},{}", midLat, midLon);
        log.info("SearchService: flatPois = {}, maxTime = {}", flatPois.size(), request.getMaxTime());
        List<CategoryDTO> structuredData = DataMapper.mapToHierarchy(flatPois);


        return new SearchResponse(request.getRequestId(), structuredData);
    }

    public RouteResponse getRoute(PointDTO from, PointDTO to) {
        try {
            RouteResponse route = ghClient.getFromToRoute(from, to);

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

    private String safeFetchIsochrone(double lat, double lon, int seconds) {
        try {
            return ghClient.fetchIsochrone(lat, lon, seconds);
        } catch (Exception e) {
            log.warn("Fallback to radius polygon", e);
            return fallbackClient.fetchIsochrone(lat, lon, seconds);
        }
    }
}
package sfedu.ictis.woi.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import sfedu.ictis.woi.infrastructure.client.GraphHopperClient;
import sfedu.ictis.woi.infrastructure.client.GraphHopperCustom;
import sfedu.ictis.woi.mapper.DataMapper;
import sfedu.ictis.woi.model.SearchResponse;
import sfedu.ictis.woi.model.SearchRequest;
import sfedu.ictis.woi.model.dto.CategoryDTO;
import sfedu.ictis.woi.projection.FlatPoiProjection;
import sfedu.ictis.woi.repository.PoiRepository;

import java.util.List;

@Service
public class SearchService {
    private static final Logger log = LoggerFactory.getLogger(SearchService.class);

    private final PoiRepository poiRepository;

    private final GraphHopperClient ghClient;
    private final GraphHopperCustom fallbackClient;

    public SearchService(PoiRepository poiRepository, GraphHopperClient ghClient, GraphHopperCustom fallbackClient) {
        this.poiRepository = poiRepository;
        this.ghClient = ghClient;
        this.fallbackClient = fallbackClient;
    }

    public SearchResponse findAllPois(SearchRequest request) {
        double midLat = (request.getP1().lat() + request.getP2().lat()) / 2;
        double midLon = (request.getP1().lon() + request.getP2().lon()) / 2;

        // можно (timeLimit - minTime)
        String isochroneWkt = safeFetchIsochrone(midLat, midLon, request.getMaxTime() * 60);

        List<FlatPoiProjection> flatPois = poiRepository.findPoisInIsochrone(
                request.getLang() != null ? request.getLang() : "ru",
                isochroneWkt
        );

        List<CategoryDTO> structuredData = DataMapper.mapToHierarchy(flatPois);

        return new SearchResponse(request.getRequestId(), structuredData);
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

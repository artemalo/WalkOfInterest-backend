package sfedu.ictis.woi.model;

import sfedu.ictis.woi.model.dto.PointDTO;

import java.util.List;

public record RouteResponse(
        long minTime,
        double distance,
        List<PointDTO> route
) {}
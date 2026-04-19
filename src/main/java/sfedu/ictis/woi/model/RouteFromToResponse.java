package sfedu.ictis.woi.model;

import sfedu.ictis.woi.model.dto.PointDTO;

import java.util.List;

public record RouteFromToResponse(
        long minTime,
        double distance,
        List<PointDTO> route
) {}
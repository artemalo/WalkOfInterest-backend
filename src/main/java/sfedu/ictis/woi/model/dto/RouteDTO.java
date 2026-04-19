package sfedu.ictis.woi.model.dto;

import java.util.List;

public record RouteDTO (
        long minTime,
        double distance,
        long steps,
        List<PointDTO> route
){}

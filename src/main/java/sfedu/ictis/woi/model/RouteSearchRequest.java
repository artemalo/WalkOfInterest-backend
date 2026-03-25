package sfedu.ictis.woi.model;

import sfedu.ictis.woi.model.dto.PointDTO;

public record RouteSearchRequest(
        PointDTO p1,
        PointDTO p2,
        int timeLimitMinutes,
        String lang,
        String requestId
) {}
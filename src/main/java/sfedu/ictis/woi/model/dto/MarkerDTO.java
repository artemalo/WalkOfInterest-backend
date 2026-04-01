package sfedu.ictis.woi.model.dto;

import java.util.List;

public record MarkerDTO(
        Long id,
        String name,
        String description,
        String lang,
        double lat,
        double lon,
        List<SubTagDTO> tags
) {}
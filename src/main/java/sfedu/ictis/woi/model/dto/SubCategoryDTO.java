package sfedu.ictis.woi.model.dto;

import java.util.List;

public record SubCategoryDTO(
        Integer id,
        String name,
        String description,
        String icon,
        List<MarkerDTO> pois
) {}
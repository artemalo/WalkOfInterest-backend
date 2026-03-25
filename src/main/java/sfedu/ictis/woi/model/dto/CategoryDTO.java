package sfedu.ictis.woi.model.dto;

import java.util.List;

public record CategoryDTO(
        Integer id,
        String name,
        String description,
        String icon,
        List<SubCategoryDTO> subcategories
) {}
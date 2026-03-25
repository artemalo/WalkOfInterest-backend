package sfedu.ictis.woi.model;

import sfedu.ictis.woi.model.dto.CategoryDTO;

import java.util.List;

public record PoisResponse(
        String requestId,
        List<CategoryDTO> categories
) {}
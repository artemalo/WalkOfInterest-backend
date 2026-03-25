package sfedu.ictis.woi.model.dto;

public record MarkerDTO(
        Long id,
        String name,
        String description,
        double lat,
        double lon
) {}
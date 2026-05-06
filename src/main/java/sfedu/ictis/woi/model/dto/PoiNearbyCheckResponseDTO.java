package sfedu.ictis.woi.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PoiNearbyCheckResponseDTO {
    private boolean existsNearby;

    private List<PoiNearbyDTO> pois;
}
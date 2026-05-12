package sfedu.ictis.woi.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PoiNearbyDTO {
    private Long id;
    private String name;
    private Integer categoryId;
    private String categoryName;
    private String subcategoryName;
    private PointDTO point;

    /**
     * Расстояние до точки запроса в метрах
     */
    private Double distanceMeters;
}
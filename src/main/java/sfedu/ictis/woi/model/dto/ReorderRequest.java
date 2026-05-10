package sfedu.ictis.woi.model.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReorderRequest {
    private PointDTO p1;
    private PointDTO p2;
    private List<ReorderPoiItem> pois;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReorderPoiItem {
        private Long id;
        private Double lat;
        private Double lon;
    }
}

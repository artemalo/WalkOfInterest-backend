package sfedu.ictis.woi.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchRequestDTO {
    private PointDTO p1;
    private PointDTO p2;
    private Integer maxTime;
}
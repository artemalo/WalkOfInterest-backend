package sfedu.ictis.woi.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import sfedu.ictis.woi.model.dto.PointDTO;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RouteRequest {
    private PointDTO p1;
    private PointDTO p2;
}

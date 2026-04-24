package sfedu.ictis.woi.model;

import lombok.Data;
import sfedu.ictis.woi.model.entity.PoiStatus;

@Data
public class UpdateStatusRequest {
    private PoiStatus status;
}

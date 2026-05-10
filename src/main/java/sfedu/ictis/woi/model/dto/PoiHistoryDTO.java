package sfedu.ictis.woi.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import sfedu.ictis.woi.model.entity.PoiHistoryAction;
import sfedu.ictis.woi.model.entity.PoiStatus;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PoiHistoryDTO {
    private Long id;
    private Long poiId;
    private String actorUsername;
    private PoiHistoryAction actionType;
    private PoiStatus oldStatus;
    private PoiStatus newStatus;
    private LocalDateTime changedAt;
    private String note;
}

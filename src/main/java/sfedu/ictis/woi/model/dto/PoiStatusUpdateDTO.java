package sfedu.ictis.woi.model.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import sfedu.ictis.woi.model.entity.PoiStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PoiStatusUpdateDTO {
    @NotNull
    private PoiStatus status;

    @Size(max = 1000)
    private String rejectionReason;
}
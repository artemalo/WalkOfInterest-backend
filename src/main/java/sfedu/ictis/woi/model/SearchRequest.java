package sfedu.ictis.woi.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import sfedu.ictis.woi.model.dto.PointDTO;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchRequest {
    @NotNull(message = "Должна быть стартовая точка")
    private PointDTO p1;

    @NotNull(message = "Должна быть финишная точка")
    private PointDTO p2;

    @Size(max = 720, message = "Время должно быть меньше 720 минут")
    private Integer maxTime;

    private String lang;
    private String requestId;
}
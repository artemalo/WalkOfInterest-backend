package sfedu.ictis.woi.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
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

    @Max(value = 720, message = "Время должно быть меньше 720 минут")
    private Integer maxTime;

    @Min(value = 1, message = "maxPoi должен быть >= 1")
    @Max(value = 50, message = "maxPoi должен быть <= 50")
    private Integer maxPoi;

    private String lang;
    private String requestId;
}
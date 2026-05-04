package sfedu.ictis.woi.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import sfedu.ictis.woi.model.entity.PoiStatus;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PoiDTO {
    private Long id;
    private String name;
    private String description;
    private String lang;
    private Double lat;
    private Double lon;
    private List<TagDTO> tags;
    private Boolean selected = false;
    private Double rate;
    private Integer count;

    private PoiStatus status;
    private boolean userGenerated;

    @JsonIgnore
    private Double interest = 0.0;

    @JsonIgnore
    private Double score = 0.0;
}
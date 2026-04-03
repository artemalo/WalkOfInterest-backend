package sfedu.ictis.woi.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private Integer selected = 0;
    private Double rate;
    private Integer count;

    @JsonIgnore
    private Double score = 0.0;
}
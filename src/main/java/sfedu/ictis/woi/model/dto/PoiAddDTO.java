package sfedu.ictis.woi.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PoiAddDTO {
    private Long id;
    private PointDTO point;

    private String name;
    private String description;
    private String lang;

    private List<Integer> subcategoriesId;
}

package sfedu.ictis.woi.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDTO {
    private Integer id;
    private String name;
    private String description;
    private String icon;
    private Integer selected = 0;
    private Integer time = 0;
    private List<SubCategoryDTO> subcategories;
}
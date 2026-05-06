package sfedu.ictis.woi.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryWithSubcategoriesDTO {
    private Integer categoryId;
    private String categoryName;
    private String categoryIcon;

    private List<SubcategoryShortDTO> subcategories;
}
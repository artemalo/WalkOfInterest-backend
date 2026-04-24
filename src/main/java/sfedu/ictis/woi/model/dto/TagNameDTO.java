package sfedu.ictis.woi.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TagNameDTO {
    Integer categoryId;
    String subcategoryName;
    TagDTO tag;
}

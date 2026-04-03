package sfedu.ictis.woi.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import sfedu.ictis.woi.model.dto.CategoryDTO;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchResponse {
    private String requestId;
    private List<CategoryDTO> categories;
}
package sfedu.ictis.woi.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import sfedu.ictis.woi.model.entity.PoiStatus;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PoiAdminDTO {
    private Long id;
    private PointDTO point;

    private String name;
    private String description;
    private String lang;

    private List<TagNameDTO> tags;

    private PoiStatus status;
    private UserDTO createdUser;
}

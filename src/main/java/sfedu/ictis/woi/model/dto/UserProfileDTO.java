package sfedu.ictis.woi.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileDTO {
    private Long id;
    private String username;
    private String firstName;
    private String lastName;
    private String bio;
    private String photoUrl;
    private Integer countTrips;
    private Integer countSpots;
    private Integer countComments;
}
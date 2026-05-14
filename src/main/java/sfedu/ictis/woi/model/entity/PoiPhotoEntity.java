package sfedu.ictis.woi.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "poi_photos")
@Getter
@Setter
public class PoiPhotoEntity {
    @Id
    private Long poiId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "poi_id")
    private PoiEntity poi;

    @Column(name = "photo_url", length = 500, nullable = false)
    private String photoUrl;
}
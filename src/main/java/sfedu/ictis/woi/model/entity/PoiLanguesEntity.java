package sfedu.ictis.woi.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import sfedu.ictis.woi.model.entity.id.PoisLanguesId;


@Entity
@Table(name = "pois_langues")
@IdClass(PoisLanguesId.class)
@Getter
@Setter
public class PoiLanguesEntity {
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poi_id")
    private PoiEntity poi;

    @Id
    @Column(name = "langue", length = 50)
    private String langue;

    @Column(name = "poi_name", columnDefinition = "text")
    private String poiName;

    @Column(name = "poi_description", columnDefinition = "text")
    private String poiDescription;

}
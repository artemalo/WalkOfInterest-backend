package sfedu.ictis.woi.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "poi_ratings")
@Getter
@Setter
public class PoiRatingsEntity {

    @Id
    @Column(name = "poi_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "poi_id")
    private PoiEntity poi;

    @Column(name = "avg_rate", nullable = false)
    private Double avgRate = 0.0;

    @Column(name = "count_rate", nullable = false)
    private Integer countRate = 0;
}
package sfedu.ictis.woi.model.entity;

import org.locationtech.jts.geom.Geometry; // Используем JTS
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "pois")
@Getter
@Setter
public class PoiEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "geometry(Geometry, 4326)", nullable = false)
    private Geometry geom;

    @Column(name = "osm_type")
    private String osmType;

    @Column(name = "osm_id")
    private Long osmId;

    @Column(name = "osm_uid")
    private Long osmUid;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "last_update", nullable = false, insertable = false, updatable = false)
    private LocalDateTime lastUpdate;
}
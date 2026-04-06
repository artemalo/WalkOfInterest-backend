package sfedu.ictis.woi.model.entity;

import org.locationtech.jts.geom.Geometry;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Column(name = "last_update", nullable = false, insertable = false, updatable = false)
    private LocalDateTime lastUpdate;

    @ManyToMany
    @JoinTable(
            name = "poi_system_tags",
            joinColumns = @JoinColumn(name = "poi_id"),
            inverseJoinColumns = @JoinColumn(name = "subcategory_id")
    )
    private Set<SubcategoryEntity> subcategories = new HashSet<>();

    public boolean isUserGenerated() {
        return this.user != null;
    }
}
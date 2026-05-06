package sfedu.ictis.woi.model.entity;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.locationtech.jts.geom.Geometry;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "pois")
@Getter
@Setter
@SQLDelete(sql = "UPDATE pois SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
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

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PoiStatus status = PoiStatus.APPROVED;

    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

    @OneToMany(mappedBy = "poi", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PoiLanguesEntity> locales = new ArrayList<>();

    @OneToOne(mappedBy = "poi", cascade = CascadeType.ALL)
    private PoiRatingsEntity rating;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;



    public boolean isUserGenerated() {
        return this.user != null;
    }
}
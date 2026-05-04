package sfedu.ictis.woi.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sfedu.ictis.woi.model.entity.PoiEntity;
import sfedu.ictis.woi.model.entity.PoiStatus;
import sfedu.ictis.woi.model.entity.UserEntity;
import sfedu.ictis.woi.projection.FlatPoiProjection;

import java.util.List;
import java.util.Optional;

@Repository
public interface PoiRepository extends JpaRepository<PoiEntity, Long> {
    @Query(value = """
        SELECT
            c.id as catId, c.category_name as catName, c.category_description as catDescription, c.category_icon as catIcon,
            s.id as subId, s.subcategory_name as subName, s.weight as weight,
            COALESCE(s.interest, 0.0) as interest,
            si.subcategory_description as subDescription, si.subcategory_icon as subIcon,
            p.id as poiId,
            p.user_id as userId,
            p.status as status,
            pl.langue as poiLang, pl.poi_name as poiName, pl.poi_description as poiDesc,
            COALESCE(pr.avg_rate, 0.0) as rate,
            COALESCE(pr.count_rate, 0) as count,
            ST_X(ST_Centroid(p.geom)) as lon,
            ST_Y(ST_Centroid(p.geom)) as lat
        FROM pois p
        JOIN poi_system_tags pst ON p.id = pst.poi_id
        JOIN subcategories s ON pst.subcategory_id = s.id
        JOIN categories c ON s.category_id = c.id
        LEFT JOIN subcategories_info si ON s.id = si.subcategory_id
        LEFT JOIN poi_ratings pr ON p.id = pr.poi_id
        LEFT JOIN LATERAL (
            SELECT
            	langue,
                poi_name,
                poi_description
            FROM pois_langues
            WHERE poi_id = p.id
            ORDER BY (
                CASE
                    WHEN langue = :lang THEN 1
                    WHEN langue = 'default' THEN 2
                    WHEN langue = 'en' THEN 3
                    ELSE 4
                END
            )
            LIMIT 1
        ) pl ON true
        WHERE
                p.deleted_at IS NULL
                AND p.status = 'APPROVED'
                AND p.geom && ST_GeomFromText(:isochroneWkt, 4326)
                AND ST_Within(p.geom, ST_GeomFromText(:isochroneWkt, 4326))
        ORDER BY s.weight DESC;
        """, nativeQuery = true)
    List<FlatPoiProjection> findPoisInIsochrone(
            @Param("lang") String lang,
            @Param("isochroneWkt") String wkt
    );

    @EntityGraph(attributePaths = {"locales", "subcategories"})
    List<PoiEntity> findAllByUser(UserEntity user);

    @Query(value = """
        SELECT EXISTS (
            SELECT 1 FROM pois
            WHERE deleted_at IS NULL
              AND ST_DWithin(geom::geography, ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography, 5)
        )
        """, nativeQuery = true)
    boolean existsNearby(@Param("lon") Double lon, @Param("lat") Double lat);



    @Query(value = "SELECT * FROM pois WHERE id = :id", nativeQuery = true)
    Optional<PoiEntity> findByIdIncludingDeleted(@Param("id") Long id);

    @Query(
            value = "SELECT * FROM pois WHERE status = :#{#status.name()}",
            countQuery = "SELECT COUNT(*) FROM pois WHERE status = :#{#status.name()}",
            nativeQuery = true
    )
    Page<PoiEntity> findAllByStatusIncludingDeleted(@Param("status") PoiStatus status, Pageable pageable);
}
package sfedu.ictis.woi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sfedu.ictis.woi.model.entity.PoiEntity;
import sfedu.ictis.woi.projection.FlatPoiProjection;

import java.util.List;

@Repository
public interface PoiRepository extends JpaRepository<PoiEntity, Long> {
    @Query(value = """
        SELECT
            c.id as cat_id, c.category_name, c.category_description, c.category_icon,
            s.id as sub_id, s.subcategory_name, s.weight,
            si.subcategory_description, si.subcategory_icon,
            p.id as poi_id,
            pl.langue, pl.poi_name, pl.poi_description,
            ST_X(ST_Centroid(p.geom)) as lon,
            ST_Y(ST_Centroid(p.geom)) as lat
        FROM pois p
        JOIN poi_system_tags pst ON p.id = pst.poi_id
        JOIN subcategories s ON pst.subcategory_id = s.id
        JOIN categories c ON s.category_id = c.id
        LEFT JOIN subcategories_info si ON s.id = si.subcategory_id
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
        WHERE ST_Within(p.geom, ST_GeomFromText(:isochroneWkt, 4326))
        ORDER BY s.weight DESC;
        """, nativeQuery = true)
    List<FlatPoiProjection> findPoisInIsochrone(
            @Param("lang") String lang,
            @Param("isochroneWkt") String wkt
            );
}

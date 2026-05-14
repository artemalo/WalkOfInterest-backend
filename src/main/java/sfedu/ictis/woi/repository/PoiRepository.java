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
import sfedu.ictis.woi.projection.PoiNearbyProjection;

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
            pph.url as photoUrl,
            ST_X(ST_Centroid(p.geom)) as lon,
            ST_Y(ST_Centroid(p.geom)) as lat
        FROM pois p
        JOIN poi_system_tags pst ON p.id = pst.poi_id
        JOIN subcategories s ON pst.subcategory_id = s.id
        JOIN categories c ON s.category_id = c.id
        LEFT JOIN subcategories_info si ON s.id = si.subcategory_id
        LEFT JOIN poi_ratings pr ON p.id = pr.poi_id
        LEFT JOIN pois_photos pph ON p.id = pph.poi_id
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

    /**
     * Поиск ближайших точек в заданном радиусе (метры).
     * Возвращаются только не удаленные, статусы APPROVED и PENDING
     */
    @Query(value = """
        SELECT
            p.id AS id,
            pl.poi_name AS name,
            s.category_id AS categoryId,
            c.category_name AS categoryName,
            s.subcategory_name AS subcategoryName,
            ST_Y(ST_Centroid(p.geom)) AS lat,
            ST_X(ST_Centroid(p.geom)) AS lon,
            ST_Distance(
                p.geom::geography,
                ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography
            ) AS distanceMeters
        FROM pois p
        LEFT JOIN poi_system_tags pst ON p.id = pst.poi_id
        LEFT JOIN subcategories s ON pst.subcategory_id = s.id
        LEFT JOIN categories c ON s.category_id = c.id
        LEFT JOIN LATERAL (
            SELECT poi_name
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
        WHERE p.deleted_at IS NULL
          AND p.status IN ('APPROVED', 'PENDING')
          AND ST_DWithin(
              p.geom::geography,
              ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography,
              :radiusMeters
          )
        ORDER BY distanceMeters ASC
        LIMIT :limit
        """, nativeQuery = true)
    List<PoiNearbyProjection> findNearbyPois(
            @Param("lon") Double lon,
            @Param("lat") Double lat,
            @Param("radiusMeters") Double radiusMeters,
            @Param("lang") String lang,
            @Param("limit") int limit
    );

    @Query("""
        SELECT COUNT(p) FROM PoiEntity p
        WHERE p.user = :user
          AND p.status = :status
        """)
    long countByUserAndStatus(@Param("user") UserEntity user, @Param("status") PoiStatus status);

    @Query(value = "SELECT * FROM pois WHERE id = :id", nativeQuery = true)
    Optional<PoiEntity> findByIdIncludingDeleted(@Param("id") Long id);

    @Query(
            value = "SELECT * FROM pois WHERE status = :#{#status.name()}",
            countQuery = "SELECT COUNT(*) FROM pois WHERE status = :#{#status.name()}",
            nativeQuery = true
    )
    Page<PoiEntity> findAllByStatusIncludingDeleted(@Param("status") PoiStatus status, Pageable pageable);

    /**
     * Фильтрация по статусу + поиск по имени/ID + тип владельца + радиус координат.
     * Используется только в админ-панели. Удаленные POI исключены.
     *
     * @param status    статус
     * @param search    подстрока для поиска в pois_langues.poi_name; null - без фильтра по имени
     * @param searchId  ID точки для прямого совпадения; null - не применяется
     * @param ownerType "OSM" | "USER" | null (без фильтра)
     * @param lat       широта центра поиска по координатам; null - без фильтра
     * @param lon       долгота центра поиска по координатам; null - без фильтра
     */
    @Query(value = """
            SELECT p.* FROM pois p
            WHERE p.deleted_at IS NULL
              AND p.status = :status
              AND (
                  :search IS NULL
                  OR (:searchId IS NOT NULL AND p.id = :searchId)
                  OR EXISTS (
                      SELECT 1 FROM pois_langues pl
                      WHERE pl.poi_id = p.id
                        AND LOWER(pl.poi_name) LIKE LOWER(CONCAT('%', :search, '%'))
                  )
              )
              AND (
                  :ownerType IS NULL
                  OR (:ownerType = 'OSM'  AND p.user_id IS NULL)
                  OR (:ownerType = 'USER' AND p.user_id IS NOT NULL)
              )
              AND (
                  :lat IS NULL OR :lon IS NULL
                  OR ST_DWithin(
                      p.geom::geography,
                      ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography,
                      150
                  )
              )
            ORDER BY p.last_update DESC
            """,
            countQuery = """
            SELECT COUNT(*) FROM pois p
            WHERE p.deleted_at IS NULL
              AND p.status = :status
              AND (
                  :search IS NULL
                  OR (:searchId IS NOT NULL AND p.id = :searchId)
                  OR EXISTS (
                      SELECT 1 FROM pois_langues pl
                      WHERE pl.poi_id = p.id
                        AND LOWER(pl.poi_name) LIKE LOWER(CONCAT('%', :search, '%'))
                  )
              )
              AND (
                  :ownerType IS NULL
                  OR (:ownerType = 'OSM'  AND p.user_id IS NULL)
                  OR (:ownerType = 'USER' AND p.user_id IS NOT NULL)
              )
              AND (
                  :lat IS NULL OR :lon IS NULL
                  OR ST_DWithin(
                      p.geom::geography,
                      ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography,
                      150
                  )
              )
            """,
            nativeQuery = true)
    Page<PoiEntity> findAllByStatusWithFilters(
            @Param("status")    String status,
            @Param("search")    String search,
            @Param("searchId")  Long searchId,
            @Param("ownerType") String ownerType,
            @Param("lat")       Double lat,
            @Param("lon")       Double lon,
            Pageable pageable
    );
}
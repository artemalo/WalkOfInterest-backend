package sfedu.ictis.woi.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sfedu.ictis.woi.model.entity.PoiAdminEntity;
import sfedu.ictis.woi.model.entity.PoiStatus;
import sfedu.ictis.woi.model.entity.UserEntity;

import java.util.List;

@Repository
public interface PoiAdminRepository extends JpaRepository<PoiAdminEntity, Long> {

    Page<PoiAdminEntity> findAllByStatus(PoiStatus status, Pageable pageable);

    @Query(value = """
        SELECT EXISTS (
            SELECT 1 FROM pois
            WHERE ST_DWithin(geom::geography, ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography, 5)
        )
        """, nativeQuery = true)
    boolean existsNearby(@Param("lon") Double lon, @Param("lat") Double lat);

    @EntityGraph(attributePaths = {"locales", "subcategories"})
    List<PoiAdminEntity> findAllByUser(UserEntity user);
}

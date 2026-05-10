package sfedu.ictis.woi.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * История изменений POI.
 *
 * DDL (выполнить вручную):
 * <pre>
 * CREATE TABLE poi_history (
 *     id            BIGSERIAL PRIMARY KEY,
 *     poi_id        BIGINT        NOT NULL,
 *     actor_username VARCHAR(255),
 *     action_type   VARCHAR(50)   NOT NULL,
 *     old_status    VARCHAR(50),
 *     new_status    VARCHAR(50)   NOT NULL,
 *     changed_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
 *     note          VARCHAR(500)
 * );
 * CREATE INDEX idx_poi_history_poi_id ON poi_history(poi_id);
 * </pre>
 */
@Entity
@Table(name = "poi_history")
@Getter
@Setter
@NoArgsConstructor
public class PoiHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** ID точки (не FK — чтобы не ломать историю после soft-delete). */
    @Column(name = "poi_id", nullable = false)
    private Long poiId;

    /** Email или username актора (пользователь или «system»). */
    @Column(name = "actor_username")
    private String actorUsername;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 50)
    private PoiHistoryAction actionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_status", length = 50)
    private PoiStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 50)
    private PoiStatus newStatus;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private LocalDateTime changedAt;

    @Column(name = "note", length = 500)
    private String note;

    @PrePersist
    protected void onCreate() {
        if (changedAt == null) {
            changedAt = LocalDateTime.now();
        }
    }
}

package parser;

import java.sql.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BatchInserter {
    private static final Logger logger = Logger.getLogger(BatchInserter.class.getName());

    private final Connection conn;
    private final int batchSize;

    // Основной список агрегированных объектов (POI + его теги + его языки)
    private final List<PoiAggregate> batch = new ArrayList<>();

    // Кэш сабкатегорий для автоматической привязки системных тегов
    private final Map<String, Integer> subcategoryCache = new HashMap<>();

    // Вспомогательные рекорды для чистоты данных
    private record PoiItem(String type, long osmId, long uid, Timestamp ts, String wkt) {}
    private record TagItem(String key, String value) {}
    private record LangItem(String lang, String name, String desc) {}

    // Контейнер, гарантирующий атомарность POI и его детей
    private static class PoiAggregate {
        final PoiItem poi;
        final List<TagItem> tags = new ArrayList<>();
        final List<LangItem> langs = new ArrayList<>();

        PoiAggregate(PoiItem poi) { this.poi = poi; }
    }

    public BatchInserter(Connection conn, int batchSize) {
        this.conn = conn;
        this.batchSize = batchSize;
        loadSubcategories();
    }

    private void loadSubcategories() {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, subcategory_key, subcategory_value FROM subcategories")) {
            while (rs.next()) {
                String tag = rs.getString("subcategory_key") + ":" + rs.getString("subcategory_value");
                subcategoryCache.put(tag, rs.getInt("id"));
            }
            logger.info("Loaded " + subcategoryCache.size() + " subcategories to cache.");
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to load subcategories", e);
        }
    }

    public void addPoi(String type, long osmId, long uid, Timestamp ts, String wkt) {
        if (batch.size() >= batchSize) {
            flush();
        }
        batch.add(new PoiAggregate(new PoiItem(type, osmId, uid, ts, wkt)));
    }

    public void addTag(long osmId, String type, String key, String value) {
        if (batch.isEmpty()) return;
        PoiAggregate last = batch.get(batch.size() - 1);
        // Проверка соответствия (защита от записи тега не тому объекту)
        if (last.poi.osmId == osmId && last.poi.type.equals(type)) {
            last.tags.add(new TagItem(key, value));
        }
    }

    public void addLang(long osmId, String type, String lang, String name, String desc) {
        if (batch.isEmpty()) return;
        PoiAggregate last = batch.get(batch.size() - 1);
        if (last.poi.osmId == osmId && last.poi.type.equals(type)) {
            last.langs.add(new LangItem(lang, name, desc));
        }
    }

    public void flush() {
        if (batch.isEmpty()) return;

        try {
            conn.setAutoCommit(false);

            Map<String, Long> idMap = new HashMap<>();

            insertPoisAndPopulateMap(idMap);

            insertChildren(idMap);

            conn.commit();
            batch.clear();
        } catch (SQLException e) {
            try { conn.rollback(); } catch (SQLException ex) { logger.severe("Rollback failed"); }
            logger.log(Level.SEVERE, "Batch flush failed! Data lost for current batch.", e);
        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException e) { /* ignore */ }
        }
    }

    private void insertPoisAndPopulateMap(Map<String, Long> idMap) throws SQLException {
        if (batch.isEmpty()) return;

        StringBuilder sql = new StringBuilder(
                "INSERT INTO pois(osm_type, osm_id, osm_uid, last_update, geom) VALUES "
        );

        for (int i = 0; i < batch.size(); i++) {
            sql.append("(?, ?, ?, ?, ST_GeomFromText(?, 4326))");
            if (i < batch.size() - 1) sql.append(", ");
        }

        sql.append(" ON CONFLICT (osm_type, osm_id) DO UPDATE SET ")
                .append("geom = EXCLUDED.geom, last_update = EXCLUDED.last_update ")
                .append("RETURNING id, osm_type, osm_id");

        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int paramIndex = 1;
            for (PoiAggregate agg : batch) {
                ps.setString(paramIndex++, agg.poi.type());
                ps.setLong(paramIndex++, agg.poi.osmId());
                ps.setLong(paramIndex++, agg.poi.uid());
                ps.setTimestamp(paramIndex++, agg.poi.ts());
                ps.setString(paramIndex++, agg.poi.wkt());
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String key = rs.getString("osm_type") + ":" + rs.getLong("osm_id");
                    idMap.put(key, rs.getLong("id"));
                }
            }
        }
    }

    private void insertChildren(Map<String, Long> idMap) throws SQLException {
        String tagSql = "INSERT INTO osm_poi_tags (poi_id, tag_key, tag_value) VALUES (?, ?, ?) " +
                "ON CONFLICT (poi_id, tag_key, tag_value) DO NOTHING";

        String langSql = "INSERT INTO pois_langues (poi_id, langue, poi_name, poi_description) " +
                "VALUES (?, ?, ?, ?) ON CONFLICT (poi_id, langue) DO UPDATE SET " +
                "poi_name = EXCLUDED.poi_name, poi_description = EXCLUDED.poi_description";

        String sysSql = "INSERT INTO poi_system_tags (poi_id, subcategory_id) VALUES (?, ?) " +
                "ON CONFLICT DO NOTHING";

        try (PreparedStatement psTag = conn.prepareStatement(tagSql);
             PreparedStatement psLang = conn.prepareStatement(langSql);
             PreparedStatement psSys = conn.prepareStatement(sysSql)) {

            for (PoiAggregate agg : batch) {
                Long internalId = idMap.get(agg.poi.type + ":" + agg.poi.osmId);
                if (internalId == null) continue;

                // Обработка тегов (обычных и системных)
                for (TagItem t : agg.tags) {
                    psTag.setLong(1, internalId);
                    psTag.setString(2, t.key);
                    psTag.setString(3, t.value);
                    psTag.addBatch();

                    // Автоматическая привязка к системе категорий
                    Integer subId = subcategoryCache.get(t.key + ":" + t.value);
                    if (subId != null) {
                        psSys.setLong(1, internalId);
                        psSys.setInt(2, subId);
                        psSys.addBatch();
                    }
                }

                // Обработка языков
                for (LangItem l : agg.langs) {
                    psLang.setLong(1, internalId);
                    psLang.setString(2, l.lang);
                    psLang.setString(3, l.name);
                    psLang.setString(4, l.desc);
                    psLang.addBatch();
                }
            }

            psTag.executeBatch();
            psLang.executeBatch();
            psSys.executeBatch();
        }
    }

    public void flushBatches() throws SQLException {
        flush();
    }
}
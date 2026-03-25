package parser;

import de.topobyte.osm4j.core.access.DefaultOsmHandler;
import de.topobyte.osm4j.core.model.iface.*;
import de.topobyte.osm4j.pbf.seq.PbfReader;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.io.WKTWriter;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.logging.*;

public class OsmPbfParser {
    private static final Logger logger = Logger.getLogger(OsmPbfParser.class.getName());

    private static final Map<String, Set<String>> WHITELIST_TAGS = Map.ofEntries(
            Map.entry("amenity", Set.of(
                    "nightclub", "casino", "theatre", "cinema", "arts_centre",
                    "conference_centre", "community_centre", "pub", "bar",
                    "biergarten", "food_court", "restaurant", "cafe",
                    "ice_cream", "marketplace", "library", "college",
                    "townhall", "courthouse", "hospital", "clinic",
                    "dentist", "pharmacy", "bank", "post_office",
                    "bus_station", "ferry_terminal", "place_of_worship",
                    "clock"
            )),
            Map.entry("leisure", Set.of(
                    "beach_resort", "stadium", "nature_reserve", "golf_course",
                    "sports_centre", "marina", "horse_riding", "park",
                    "swimming_pool", "garden", "fitness_centre", "pitch",
                    "track", "dog_park", "slipway"
            )),
            Map.entry("sport", Set.of(
                    "golf", "skiing", "surfing", "sailing", "ice_hockey",
                    "ice_skating", "climbing", "horse_racing", "equestrian",
                    "tennis", "swimming", "athletics", "soccer", "basketball",
                    "volleyball", "running", "cycling", "fitness", "yoga",
                    "rowing"
            )),
            Map.entry("tourism", Set.of(
                    "theme_park", "zoo", "aquarium", "museum", "gallery",
                    "viewpoint", "attraction", "artwork", "hotel", "hostel",
                    "motel", "guest_house", "alpine_hut", "wilderness_hut",
                    "camp_site", "picnic_site"
            )),
            Map.entry("shop", Set.of(
                    "mall", "jewelry", "electronics", "supermarket",
                    "convenience", "bakery", "butcher", "greengrocer",
                    "deli", "clothes", "shoes", "sports", "furniture",
                    "garden_centre", "doityourself", "bicycle", "music",
                    "antiques", "second_hand", "beauty", "travel_agency",
                    "kiosk"
            )),
            Map.entry("historic", Set.of(
                    "heritage", "castle", "archaeological_site", "monument",
                    "fort", "tower", "manor", "church", "city_gate",
                    "ship", "watermill", "ruins", "battlefield",
                    "cemetery", "tomb", "memorial", "pillory",
                    "boundary_stone", "wayside_shrine", "mine"
            )),
            Map.entry("artwork_type", Set.of(
                    "sculpture", "statue", "installation", "relief",
                    "bust", "mosaic", "mural", "graffiti", "stone"
            )),
            Map.entry("memorial", Set.of(
                    "bust"
            )),
            Map.entry("man_made", Set.of(
                    "bridge", "obelisk", "observatory", "ceremonial_gate",
                    "tower", "lighthouse", "watermill", "windmill",
                    "pier", "breakwater", "geoglyph", "cross",
                    "maypole", "water_tower", "ruins",
                    "mineshaft", "adit", "gasometer"
            )),
            Map.entry("building", Set.of(
                    "cathedral", "palace", "castle", "university",
                    "train_station", "church", "mosque", "temple",
                    "synagogue", "chapel", "fire_station",
                    "dormitory", "factory", "ruins"
            )),
            Map.entry("natural", Set.of(
                    "geyser", "hot_spring", "gorge", "bay", "beach",
                    "cave_entrance", "spring", "hill", "isthmus", "stone"
            )),
            Map.entry("landuse", Set.of(
                    "recreation_ground", "brownfield"
            )),
            Map.entry("waterway", Set.of(
                    "waterfall"
            )),
            Map.entry("water", Set.of(
                    "reservoir", "pond"
            ))
    );

    private final Map<Long, Coordinate> nodeCoords = new HashMap<>();
    private final Map<Long, OsmWay> ways = new HashMap<>();

    private final BatchInserter batchInserter;

    private final Connection conn;
    private int processedCount = 0;
    private static final int BATCH_SIZE = 1000;
    private static final int PROGRESS_INTERVAL = 100_000;

    // Для JTS
    private final GeometryFactory geometryFactory = new GeometryFactory();

    public OsmPbfParser(Connection conn) {
        this.conn = conn;
        this.batchInserter = new BatchInserter(conn, BATCH_SIZE);

        setupFileLogging();
    }

    private static class ProgressLogger {
        private final String entityType;
        private final long entityId;
        private final int totalMembers;
        private int processedMembers = 0;
        private final long startTime = System.currentTimeMillis();

        public ProgressLogger(String entityType, long entityId, int totalMembers) {
            this.entityType = entityType;
            this.entityId = entityId;
            this.totalMembers = totalMembers;
            logger.info("Starting processing " + entityType + " " + entityId + " with " + totalMembers + " members");
        }

        public void increment() {
            processedMembers++;
            if (processedMembers % 100 == 0) {
                double progress = (double) processedMembers / totalMembers * 100;
                long elapsed = System.currentTimeMillis() - startTime;
                logger.info(entityType + " " + entityId + ": " + processedMembers +
                        "/" + totalMembers + " (" + String.format("%.1f", progress) + "%) - " +
                        elapsed + "ms elapsed");
            }
        }

        public void complete() {
            long elapsed = System.currentTimeMillis() - startTime;
            logger.info("Completed processing " + entityType + " " + entityId +
                    " in " + elapsed + "ms");
        }
    }

    public void parse(String filePath) throws Exception {
        logger.info("Starting parsing of file: " + filePath);
        PbfReader reader = new PbfReader(new FileInputStream(filePath), false);

        DefaultOsmHandler handler = new DefaultOsmHandler() {
            @Override
            public void handle(OsmNode node) {
                if (hasValidTag(node)) {
                    processNode(node);
                }

                nodeCoords.put(node.getId(), new Coordinate(node.getLongitude(), node.getLatitude()));
                checkProgress();
            }

            @Override
            public void handle(OsmWay way) {
                if (hasValidTag(way)) {
                    processWay(way);
                }

                ways.put(way.getId(), way);
                checkProgress();
            }

            @Override
            public void handle(OsmRelation relation) {
                if (hasValidTag(relation)) {
                    processRelation(relation);
                }
                checkProgress();
            }
        };

        reader.setHandler(handler);
        reader.read();

        batchInserter.flushBatches();

        clearCaches();

        logger.info("Parsing complete. Total processed: " + processedCount);
    }

    private void checkProgress() {
        processedCount++;
        if (processedCount % PROGRESS_INTERVAL == 0) {
            logger.info("Processed " + processedCount + " objects so far");
        }
    }

    private boolean hasValidTag(OsmEntity entity) {
        for (int i = 0; i < entity.getNumberOfTags(); i++) {
            OsmTag tag = entity.getTag(i);
            String key = tag.getKey();
            String value = tag.getValue().toLowerCase();

            if (WHITELIST_TAGS.containsKey(key) &&
                    WHITELIST_TAGS.get(key).contains(value)) {
                return true;
            }
        }
        return false;
    }

    private void processNode(OsmNode node) {
        try {
            String geom = createPointGeometry(node.getLongitude(), node.getLatitude());
            // Добавляем саму точку
            batchInserter.addPoi("node", node.getId(), getOsmUid(node), getTimestampFromOsm(node), geom);

            // Добавляем теги (теперь с типом "node")
            for (int i = 0; i < node.getNumberOfTags(); i++) {
                OsmTag tag = node.getTag(i);
                batchInserter.addTag(node.getId(), "node", tag.getKey(), tag.getValue());
            }

            // Добавляем языковые данные (теперь с типом "node")
            Map<String, Map<String, String>> langData = extractLangData(node);
            for (Map.Entry<String, Map<String, String>> entry : langData.entrySet()) {
                String lang = entry.getKey();
                Map<String, String> data = entry.getValue();
                batchInserter.addLang(node.getId(), "node", lang,
                        data.getOrDefault("name", null),
                        data.getOrDefault("description", null));
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error processing node " + node.getId(), e);
        }
    }

    private void processWay(OsmWay way) {
        if (way.getNumberOfNodes() < 4) return;

        List<Coordinate> coordinates = buildWayCoordinates(way);
        if (coordinates == null) return;

        Polygon polygon = geometryFactory.createPolygon(coordinates.toArray(new Coordinate[0]));

        try {
            String geom = createPolygonGeometry(polygon);
            batchInserter.addPoi("way", way.getId(), getOsmUid(way), getTimestampFromOsm(way), geom);

            for (int i = 0; i < way.getNumberOfTags(); i++) {
                OsmTag tag = way.getTag(i);
                batchInserter.addTag(way.getId(), "way", tag.getKey(), tag.getValue());
            }

            Map<String, Map<String, String>> langData = extractLangData(way);
            for (Map.Entry<String, Map<String, String>> entry : langData.entrySet()) {
                batchInserter.addLang(way.getId(), "way", entry.getKey(),
                        entry.getValue().getOrDefault("name", null),
                        entry.getValue().getOrDefault("description", null));
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error processing way " + way.getId(), e);
        }
    }

    private void processRelation(OsmRelation relation) {
        // является ли отношение мультиполигоном
        boolean isMultipolygon = false;
        for (int i = 0; i < relation.getNumberOfTags(); i++) {
            OsmTag tag = relation.getTag(i);
            if ("type".equals(tag.getKey()) && "multipolygon".equals(tag.getValue())) {
                isMultipolygon = true;
                break;
            }
        }

        if (!isMultipolygon) return;

        // логгер прогресса для тяжелых объектов
        ProgressLogger progressLogger =
                new ProgressLogger("relation", relation.getId(), relation.getNumberOfMembers());

        List<Polygon> outerPolygons = new ArrayList<>();

        // Собираем геометрию из путей (ways), которые должны быть в кэше
        for (int i = 0; i < relation.getNumberOfMembers(); i++) {
            progressLogger.increment();

            OsmRelationMember member = relation.getMember(i);

            if (member.getType() != EntityType.Way) continue;

            OsmWay way = ways.get(member.getId());
            if (way == null) continue;

            List<Coordinate> coordinates = buildWayCoordinates(way);
            if (coordinates == null) continue;

            try {
                outerPolygons.add(
                        geometryFactory.createPolygon(coordinates.toArray(new Coordinate[0]))
                );
            } catch (Exception e) {
                logger.warning("Could not create polygon for way " + way.getId() + " in relation " + relation.getId());
            }
        }

        if (outerPolygons.isEmpty()) {
            progressLogger.complete();
            return;
        }

        try {
            // MultiPolygon через JTS
            MultiPolygon multiPolygon = geometryFactory.createMultiPolygon(
                    outerPolygons.toArray(new Polygon[0])
            );

            String geom = createMultiPolygonGeometry(multiPolygon);

            batchInserter.addPoi("relation",
                    relation.getId(),
                    getOsmUid(relation),
                    getTimestampFromOsm(relation),
                    geom);

            for (int i = 0; i < relation.getNumberOfTags(); i++) {
                OsmTag tag = relation.getTag(i);
                batchInserter.addTag(relation.getId(), "relation", tag.getKey(), tag.getValue());
            }

            Map<String, Map<String, String>> langData = extractLangData(relation);
            for (Map.Entry<String, Map<String, String>> entry : langData.entrySet()) {
                String lang = entry.getKey();
                Map<String, String> data = entry.getValue();

                batchInserter.addLang(
                        relation.getId(),
                        "relation",
                        lang,
                        data.getOrDefault("name", null),
                        data.getOrDefault("description", null)
                );
            }

            progressLogger.complete();

        } catch (Exception e) {
            logger.log(Level.SEVERE,
                    "Error finalizing geometry for relation " + relation.getId() + ": " + e.getMessage());
        }
    }

    private String createPointGeometry(double lon, double lat) {
        return String.format(Locale.US, "POINT(%f %f)", lon, lat);
    }

    private String createPolygonGeometry(Polygon polygon) {
        WKTWriter writer = new WKTWriter();
        return writer.write(polygon);
    }

    private String createMultiPolygonGeometry(MultiPolygon multiPolygon) {
        WKTWriter writer = new WKTWriter();
        return writer.write(multiPolygon);
    }

    private List<Coordinate> buildWayCoordinates(OsmWay way) {
        List<Coordinate> coordinates = new ArrayList<>();

        for (int i = 0; i < way.getNumberOfNodes(); i++) {
            Coordinate c = nodeCoords.get(way.getNodeId(i));

            if (c == null) {
                logger.warning("Missing node " + way.getNodeId(i) + " for way" + way.getId());
                return null;
            }

            coordinates.add(c);
        }

        // замкнутость полигона
        if (!coordinates.get(0).equals2D(coordinates.get(coordinates.size() - 1))) {
            return null;
        }

        return coordinates;
    }


    private Map<String, Map<String, String>> extractLangData(OsmEntity entity) {
        Map<String, Map<String, String>> result = new HashMap<>();

        for (int i = 0; i < entity.getNumberOfTags(); i++) {
            OsmTag tag = entity.getTag(i);
            String key = tag.getKey();
            String value = tag.getValue();

            if (key.startsWith("name:") || key.startsWith("description:")) {
                String[] parts = key.split(":", 2);
                if (parts.length >= 2) {
                    String lang = parts[1];
                    String type = parts[0];

                    result.computeIfAbsent(lang, k -> new HashMap<>())
                            .put(type, value);
                }
            } else if ("name".equals(key) || "description".equals(key)) {
                result.computeIfAbsent("default", k -> new HashMap<>())
                        .put(key, value);
            }
        }

        return result;
    }
    private Timestamp getTimestampFromOsm(OsmEntity entity) {
        OsmMetadata osmMetadata = entity.getMetadata();

        if (osmMetadata != null) {
            return new Timestamp(osmMetadata.getTimestamp());
        }
        return new Timestamp(System.currentTimeMillis());
    }

    private long getOsmUid(OsmEntity entity) {
        OsmMetadata meta = entity.getMetadata();
        return (meta != null) ? meta.getUid() : 0L;
    }

    public void clearCaches() {
        int nodeCount = nodeCoords.size();
        int wayCount = ways.size();

        nodeCoords.clear();
        ways.clear();

        logger.info("Cache cleared: " + nodeCount + " nodes and " + wayCount +
                " ways removed. Total processed: " + processedCount);
    }

    public Map<String, Integer> getStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("processed_objects", processedCount);
        stats.put("cached_nodes", nodeCoords.size());
        stats.put("cached_ways", ways.size());
        return stats;
    }

    public void close() throws SQLException {
        for (Handler handler : logger.getHandlers()) {
            handler.close();
        }

        if (conn != null && !conn.isClosed()) {
            conn.close();
            logger.info("Database connection closed");
        }
    }

    private void setupFileLogging() {
        try {
            FileHandler fileHandler = new FileHandler("osm_parser.log", 512 * 1024 * 1024, 1, true);
            fileHandler.setFormatter(new SimpleFormatter());
            fileHandler.setLevel(Level.INFO);
            logger.addHandler(fileHandler);
        } catch (IOException e) {
            System.err.println("Ошибка настройки файлового логгера: " + e.getMessage());
        }
    }
}
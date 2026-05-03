package sfedu.ictis.woi.service.selection;

import sfedu.ictis.woi.model.dto.PointDTO;

/**
 * Геометрия в WGS84 без проекций. Все расстояния - метры
 */
public final class GeoUtils {
    private GeoUtils() {}
    public static final double EARTH_RADIUS_M = 6_371_000.0;
    public static final double DETOUR_FACTOR = 1.35;
    public static final double WALK_SPEED_M_PER_MIN = 80.0;

    public static double haversine(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double rLat1 = Math.toRadians(lat1);
        double rLat2 = Math.toRadians(lat2);

        double sinDLat = Math.sin(dLat / 2);
        double sinDLon = Math.sin(dLon / 2);

        double a = sinDLat * sinDLat + Math.cos(rLat1) * Math.cos(rLat2) * sinDLon * sinDLon;
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_M * c;
    }

    public static double haversine(PointDTO a, PointDTO b) {
        return haversine(a.getLat(), a.getLon(), b.getLat(), b.getLon());
    }

    /**
     * Минимальное расстояние от точки P до отрезка AB, в метрах
     */
    public static double distanceToSegment(double pLat, double pLon,
                                           double aLat, double aLon,
                                           double bLat, double bLon) {
        // Локальная плоскость с центром между A и B
        double midLat = (aLat + bLat) * 0.5;
        double mLatPerDeg = 111_320.0; // м на градус широты
        double mLonPerDeg = 111_320.0 * Math.cos(Math.toRadians(midLat)); // м на градус долготы

        double ax = (aLon - midLat == 0 ? 0 : (aLon * mLonPerDeg));
        double ay = aLat * mLatPerDeg;
        double bx = bLon * mLonPerDeg;
        double by = bLat * mLatPerDeg;
        double px = pLon * mLonPerDeg;
        double py = pLat * mLatPerDeg;

        double dx = bx - ax;
        double dy = by - ay;
        double len2 = dx * dx + dy * dy;

        if (len2 < 1e-9) {
            // A и B совпали — расстояние до точки A
            double ddx = px - ax;
            double ddy = py - ay;
            return Math.sqrt(ddx * ddx + ddy * ddy);
        }

        double t = ((px - ax) * dx + (py - ay) * dy) / len2;
        t = Math.max(0.0, Math.min(1.0, t));

        double projX = ax + t * dx;
        double projY = ay + t * dy;

        double ex = px - projX;
        double ey = py - projY;
        return Math.sqrt(ex * ex + ey * ey);
    }

    public static boolean insideEllipse(double pLat, double pLon,
                                        double aLat, double aLon,
                                        double bLat, double bLon,
                                        double majorAxisMeters) {
        double pa = haversine(pLat, pLon, aLat, aLon);
        double pb = haversine(pLat, pLon, bLat, bLon);
        return (pa + pb) * DETOUR_FACTOR <= majorAxisMeters;
    }

    public static double walkMinutes(double meters) {
        return (meters * DETOUR_FACTOR) / WALK_SPEED_M_PER_MIN;
    }
}
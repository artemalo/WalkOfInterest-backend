package sfedu.ictis.woi.service.selection;

import lombok.extern.slf4j.Slf4j;
import sfedu.ictis.woi.model.dto.PointDTO;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Сборщик маршрута. Принимает список кандидатов
 */
@Slf4j
public class RouteAssembler {
    private final PointDTO start;
    private final PointDTO end;
    private final double hardBudgetMin;
    private final int maxPois;

    public RouteAssembler(PointDTO start, PointDTO end, double hardBudgetMin, int maxPois) {
        this.start = start;
        this.end = end;
        this.hardBudgetMin = hardBudgetMin;
        this.maxPois = maxPois;
    }

    public List<Candidate> assemble(List<Candidate> candidatesByScore) {
        List<Candidate> route = new ArrayList<>();
        Set<Long> chosen = new HashSet<>();

        double currentMinutes = GeoUtils.walkMinutes(
                GeoUtils.haversine(start.getLat(), start.getLon(), end.getLat(), end.getLon())
        );

        int rejectedByBudget = 0;
        int rejectedByDuplicate = 0;

        for (Candidate c : candidatesByScore) {
            if (route.size() >= maxPois) break;
            if (!chosen.add(c.id())) {
                rejectedByDuplicate++;
                continue;
            }

            InsertionResult best = findBestInsertion(route, c);
            double newTotal = currentMinutes + best.deltaMinutes;

            if (newTotal <= hardBudgetMin) {
                route.add(best.position, c);
                currentMinutes = newTotal;
            } else {
                chosen.remove(c.id());
                rejectedByBudget++;
            }
        }

        log.debug("RouteAssembler greedy: picked={}, rejectedByBudget={}, rejectedByDup={}, time={}min",
                route.size(), rejectedByBudget, rejectedByDuplicate, String.format("%.1f", currentMinutes));

        currentMinutes = twoOpt(route, currentMinutes);

        log.debug("RouteAssembler after 2-opt: time={}min", String.format("%.1f", currentMinutes));

        return route;
    }



    private InsertionResult findBestInsertion(List<Candidate> route, Candidate c) {
        int n = route.size();
        double bestDelta = Double.MAX_VALUE;
        int bestPos = 0;

        for (int i = 0; i <= n; i++) {
            double leftLat, leftLon, rightLat, rightLon;

            if (i == 0) {
                leftLat = start.getLat(); leftLon = start.getLon();
            } else {
                Candidate left = route.get(i - 1);
                leftLat = left.lat(); leftLon = left.lon();
            }

            if (i == n) {
                rightLat = end.getLat(); rightLon = end.getLon();
            } else {
                Candidate right = route.get(i);
                rightLat = right.lat(); rightLon = right.lon();
            }

            // delta = walk(left>c) + walk(c>right) − walk(left>right)
            double oldEdge = GeoUtils.walkMinutes(GeoUtils.haversine(leftLat, leftLon, rightLat, rightLon));
            double newLeft  = GeoUtils.walkMinutes(GeoUtils.haversine(leftLat, leftLon, c.lat(), c.lon()));
            double newRight = GeoUtils.walkMinutes(GeoUtils.haversine(c.lat(), c.lon(), rightLat, rightLon));
            double delta = newLeft + newRight - oldEdge;

            if (delta < bestDelta) {
                bestDelta = delta;
                bestPos = i;
            }
        }
        return new InsertionResult(bestPos, bestDelta);
    }

    private double twoOpt(List<Candidate> route, double currentMinutes) {
        int n = route.size();
        if (n < 2) return currentMinutes;

        boolean improved = true;
        int iterations = 0;
        int maxIterations = 50;

        while (improved && iterations++ < maxIterations) {
            improved = false;
            double bestGain = 0.0;
            int bestI = -1, bestJ = -1;

            for (int i = 0; i < n - 1; i++) {
                for (int j = i + 1; j < n; j++) {
                    double gain = computeReverseGain(route, i, j);
                    if (gain > bestGain + 1e-6) {
                        bestGain = gain;
                        bestI = i; bestJ = j;
                    }
                }
            }

            if (bestI >= 0) {
                reverse(route, bestI, bestJ);
                currentMinutes -= bestGain;
                improved = true;
            }
        }
        return currentMinutes;
    }

    /** Прирост (положительный = улучшение) от реверса route[i..j]. */
    private double computeReverseGain(List<Candidate> route, int i, int j) {
        int n = route.size();

        double leftLat  = (i == 0) ? start.getLat() : route.get(i - 1).lat();
        double leftLon  = (i == 0) ? start.getLon() : route.get(i - 1).lon();
        double rightLat = (j == n - 1) ? end.getLat() : route.get(j + 1).lat();
        double rightLon = (j == n - 1) ? end.getLon() : route.get(j + 1).lon();

        Candidate first = route.get(i);
        Candidate last  = route.get(j);

        double oldLeft  = GeoUtils.walkMinutes(GeoUtils.haversine(leftLat, leftLon, first.lat(), first.lon()));
        double oldRight = GeoUtils.walkMinutes(GeoUtils.haversine(last.lat(), last.lon(), rightLat, rightLon));

        double newLeft  = GeoUtils.walkMinutes(GeoUtils.haversine(leftLat, leftLon, last.lat(), last.lon()));
        double newRight = GeoUtils.walkMinutes(GeoUtils.haversine(first.lat(), first.lon(), rightLat, rightLon));

        return (oldLeft + oldRight) - (newLeft + newRight);
    }

    private void reverse(List<Candidate> list, int i, int j) {
        while (i < j) {
            Candidate tmp = list.get(i);
            list.set(i, list.get(j));
            list.set(j, tmp);
            i++; j--;
        }
    }

    private record InsertionResult(int position, double deltaMinutes) {}
}
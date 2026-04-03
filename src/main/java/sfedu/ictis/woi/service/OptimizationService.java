package sfedu.ictis.woi.service;

import org.springframework.stereotype.Service;
import sfedu.ictis.woi.config.OptimizerConfig;
import sfedu.ictis.woi.infrastructure.client.GraphHopperClient;
import sfedu.ictis.woi.model.SearchResponse;
import sfedu.ictis.woi.model.dto.CategoryDTO;
import sfedu.ictis.woi.model.dto.PoiDTO;
import sfedu.ictis.woi.model.dto.SubCategoryDTO;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class OptimizationService {
    private final OptimizerConfig config;
    private final ScoreCalculator scoreCalculator;

    private final GraphHopperClient ghClient;

    public OptimizationService(OptimizerConfig config, ScoreCalculator scoreCalculator, GraphHopperClient ghClient) {
        this.config = config;
        this.scoreCalculator = scoreCalculator;
        this.ghClient = ghClient;
    }

    public void optimize(SearchResponse response, Integer userMaxTime) {
        for (CategoryDTO cat : response.getCategories()) {
            for (SubCategoryDTO sub : cat.getSubcategories()) {
                for (PoiDTO poi : sub.getPois()) {
                    poi.setScore(scoreCalculator.calculatePoiScore(poi, 1.0));
                }
                sub.getPois().sort(Comparator.comparing(PoiDTO::getScore).reversed());
                sub.setScore(scoreCalculator.calculateSubcategoryScore(sub));
            }
            cat.getSubcategories().sort(Comparator.comparing(SubCategoryDTO::getScore).reversed());
        }

        List<PoiDTO> selectedPois = new ArrayList<>();

        response.getCategories().stream()
                .limit(config.getMaxCategories())
                .forEach(cat -> {
                    cat.setSelected(1);

                    cat.getSubcategories().stream()
                            .limit(config.getMaxSubcategories())
                            .forEach(sub -> {
                                if (!sub.getPois().isEmpty()) {
                                    PoiDTO topPoi = sub.getPois().getFirst();
                                    topPoi.setSelected(1);
                                    selectedPois.add(topPoi);
                                }
                            });
                });

        // Балансировка
        fitToTimeLimit(response, selectedPois, userMaxTime);
    }

    private void fitToTimeLimit(SearchResponse response, List<PoiDTO> selectedPois, int maxT) {
        if (selectedPois.isEmpty()) return;
        selectedPois.sort(Comparator.comparing(PoiDTO::getScore).reversed());

        int left = 1;
        int right = selectedPois.size();
        int best = 0;

        while (left <= right) {
            int mid = left + (right - left) / 2;

            List<PoiDTO> subset = selectedPois.subList(0, mid);
            long time = ghClient.calculateRouteTime(subset); // Могут ли индексы поплыть?

            if (time <= maxT) {
                best = mid;
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }

        for (int i = 0; i < selectedPois.size(); i++) {
            if (i < best) {
                selectedPois.get(i).setSelected(1);
            } else {
                selectedPois.get(i).setSelected(0);
            }
        }

//        if (selectedPois.size() > best) {
//            selectedPois.subList(best, selectedPois.size()).clear();
//        }

        finalizeCategories(response, selectedPois);
    }

    private void finalizeCategories(SearchResponse response, List<PoiDTO> selectedPois) {
        for (CategoryDTO cat : response.getCategories()) {
            boolean hasActive = cat.getSubcategories().stream()
                    .flatMap(s -> s.getPois().stream())
                    .anyMatch(p -> p.getSelected() == 1);

            cat.setSelected(hasActive ? 1 : 0);

            if (hasActive) {
                cat.setTime(22); // заглушка
            }
        }
    }
}
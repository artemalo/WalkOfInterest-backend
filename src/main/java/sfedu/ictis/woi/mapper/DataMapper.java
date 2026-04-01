package sfedu.ictis.woi.mapper;

import sfedu.ictis.woi.model.dto.CategoryDTO;
import sfedu.ictis.woi.model.dto.MarkerDTO;
import sfedu.ictis.woi.model.dto.SubCategoryDTO;
import sfedu.ictis.woi.model.dto.SubTagDTO;
import sfedu.ictis.woi.projection.FlatPoiProjection;

import java.util.*;
import java.util.stream.Collectors;

public class DataMapper {

    public static List<CategoryDTO> mapToHierarchy(List<FlatPoiProjection> flatPois) {
        // все строки по poiId, чтобы собрать все его подкатегории
        Map<Long, List<FlatPoiProjection>> poiGroups = flatPois.stream()
                .collect(Collectors.groupingBy(
                        FlatPoiProjection::getPoiId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        // по весу для иерархии + список всех тегов
        record RichPoi(FlatPoiProjection winner, List<SubTagDTO> allTags) {}

        List<RichPoi> richPois = poiGroups.values().stream()
                .map(list -> {
                    // макс вес
                    FlatPoiProjection winner = list.stream()
                            .max(Comparator.comparing(FlatPoiProjection::getWeight))
                            .orElseThrow();

                    // POI в список тегов
                    List<SubTagDTO> tags = list.stream()
                            .map(p -> new SubTagDTO(p.getSubId(), p.getWeight()))
                            .distinct() // -дубли
                            .toList();

                    return new RichPoi(winner, tags);
                })
                .toList();

        return richPois.stream()
                .collect(Collectors.groupingBy(
                        rp -> new CategoryKey(
                                rp.winner().getCatId(),
                                rp.winner().getCatName(),
                                rp.winner().getCatDescription(),
                                rp.winner().getCatIcon()
                        ),
                        LinkedHashMap::new,
                        Collectors.groupingBy(
                                rp -> new SubCategoryKey(
                                        rp.winner().getSubId(),
                                        rp.winner().getSubName(),
                                        rp.winner().getSubDescription(),
                                        rp.winner().getSubIcon()
                                ),
                                LinkedHashMap::new,
                                Collectors.toList()
                        )
                ))
                .entrySet().stream()
                .map(catEntry -> {
                    var catKey = catEntry.getKey();
                    List<SubCategoryDTO> subDTOs = catEntry.getValue().entrySet().stream()
                            .map(subEntry -> {
                                var subKey = subEntry.getKey();
                                List<MarkerDTO> markers = subEntry.getValue().stream()
                                        .map(rp -> new MarkerDTO(
                                                rp.winner().getPoiId(),
                                                rp.winner().getPoiName(),
                                                rp.winner().getPoiDesc(),
                                                rp.winner().getLat(),
                                                rp.winner().getLon(),
                                                rp.allTags()
                                        ))
                                        .toList();

                                return new SubCategoryDTO(subKey.id(), subKey.name(), subKey.description(), subKey.icon(), markers);
                            })
                            .toList();

                    return new CategoryDTO(catKey.id(), catKey.name(), catKey.description(), catKey.icon(), subDTOs);
                })
                .toList();
    }

    private record CategoryKey(Integer id, String name, String description, String icon) {}
    private record SubCategoryKey(Integer id, String name, String description, String icon) {}
}
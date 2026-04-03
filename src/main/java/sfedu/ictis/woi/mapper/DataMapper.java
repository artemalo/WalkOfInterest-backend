package sfedu.ictis.woi.mapper;

import sfedu.ictis.woi.model.dto.*;
import sfedu.ictis.woi.projection.FlatPoiProjection;

import java.util.*;
import java.util.stream.Collectors;

public class DataMapper {

    public static List<CategoryDTO> mapToHierarchy(List<FlatPoiProjection> flatPois) {
        Map<Long, List<FlatPoiProjection>> poiGroups = flatPois.stream()
                .collect(Collectors.groupingBy(
                        FlatPoiProjection::getPoiId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        record RichPoi(FlatPoiProjection winner, List<TagDTO> allTags) {}

        List<RichPoi> richPois = poiGroups.values().stream()
                .map(list -> {
                    FlatPoiProjection winner = list.stream()
                            .max(Comparator.comparing(FlatPoiProjection::getWeight))
                            .orElseThrow();

                    List<TagDTO> tags = list.stream()
                            .map(p -> new TagDTO(p.getSubId(), p.getWeight()))
                            .distinct()
                            .toList();

                    return new RichPoi(winner, tags);
                })
                .toList();

        // 3. Собираем иерархию: Category -> SubCategory -> Poi
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

                    // Создаем список SubCategoryDTO
                    List<SubCategoryDTO> subDTOs = catEntry.getValue().entrySet().stream()
                            .map(subEntry -> {
                                var subKey = subEntry.getKey();

                                // Создаем список PoiDTO (бывшие MarkerDTO)
                                List<PoiDTO> pois = subEntry.getValue().stream()
                                        .map(rp -> {
                                            PoiDTO poi = new PoiDTO();
                                            poi.setId(rp.winner().getPoiId());
                                            poi.setName(rp.winner().getPoiName());
                                            poi.setDescription(rp.winner().getPoiDesc());
                                            poi.setLang(rp.winner().getPoiLang());
                                            poi.setLat(rp.winner().getLat());
                                            poi.setLon(rp.winner().getLon());
                                            poi.setTags(rp.allTags());

                                            poi.setRate(rp.winner().getRate());
                                            poi.setCount(rp.winner().getCount());

                                            poi.setSelected(0);
                                            poi.setScore(0.0);
                                            return poi;
                                        })
                                        .collect(Collectors.toList());

                                SubCategoryDTO sub = new SubCategoryDTO();
                                sub.setId(subKey.id());
                                sub.setName(subKey.name());
                                sub.setDescription(subKey.description());
                                sub.setIcon(subKey.icon());
                                sub.setPois(pois);
                                sub.setScore(0.0);
                                return sub;
                            })
                            .collect(Collectors.toList());

                    // Создаем CategoryDTO
                    CategoryDTO cat = new CategoryDTO();
                    cat.setId(catKey.id());
                    cat.setName(catKey.name());
                    cat.setDescription(catKey.description());
                    cat.setIcon(catKey.icon());
                    cat.setSubcategories(subDTOs);
                    cat.setSelected(0);
                    cat.setTime(0);
                    return cat;
                })
                .collect(Collectors.toList());
    }

    private record CategoryKey(Integer id, String name, String description, String icon) {}
    private record SubCategoryKey(Integer id, String name, String description, String icon) {}
}
package sfedu.ictis.woi.mapper;

import sfedu.ictis.woi.model.dto.CategoryDTO;
import sfedu.ictis.woi.model.dto.MarkerDTO;
import sfedu.ictis.woi.model.dto.SubCategoryDTO;
import sfedu.ictis.woi.projection.FlatPoiProjection;

import java.util.Collection;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

public class DataMapper {
    public static List<CategoryDTO> mapToHierarchy(List<FlatPoiProjection> flatPois) {
        Collection<FlatPoiProjection> uniquePois = flatPois.stream()
                .collect(Collectors.toMap(
                        FlatPoiProjection::getPoiId,
                        p -> p,
                        // у кого вес больше
                        (p1, p2) -> p1.getWeight() >= p2.getWeight() ? p1 : p2,
                        LinkedHashMap::new // порядок
                ))
                .values();

        return uniquePois.stream()
                .collect(Collectors.groupingBy(
                        p -> new CategoryKey(p.getCatId(), p.getCatName(), p.getCatDescription(), p.getCatIcon()),
                        LinkedHashMap::new,
                        Collectors.groupingBy(
                                p -> new SubCategoryKey(p.getSubId(), p.getSubName(), p.getSubDescription(), p.getSubIcon()),
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

                                List<MarkerDTO> pois = subEntry.getValue().stream()
                                        .map(p -> new MarkerDTO(
                                                p.getPoiId(),
                                                p.getPoiName(),
                                                p.getPoiDesc(),
                                                p.getLat(),
                                                p.getLon()
                                        ))
                                        .toList();

                                return new SubCategoryDTO(
                                        subKey.id(),
                                        subKey.name(),
                                        subKey.description(),
                                        subKey.icon(),
                                        pois
                                );
                            })
                            .toList();

                    return new CategoryDTO(
                            catKey.id(),
                            catKey.name(),
                            catKey.description(),
                            catKey.icon(),
                            subDTOs
                    );
                })
                .toList();
    }

    private record CategoryKey(Integer id, String name, String description, String icon) {}
    private record SubCategoryKey(Integer id, String name, String description, String icon) {}
}
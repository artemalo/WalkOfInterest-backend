package sfedu.ictis.woi.mapper;

import lombok.extern.slf4j.Slf4j;
import sfedu.ictis.woi.exception.PoiNotPointException;
import sfedu.ictis.woi.model.dto.PoiDTO;
import sfedu.ictis.woi.model.dto.TagDTO;
import sfedu.ictis.woi.model.entity.PoiEntity;
import sfedu.ictis.woi.model.entity.PoisLanguesEntity;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class PoiMapper {
    public static PoiDTO mapToDTO(PoiEntity entity, String targetLang) {
        PoiDTO dto = new PoiDTO();
        dto.setId(entity.getId());

        if (entity.getGeom() instanceof org.locationtech.jts.geom.Point point) {
            dto.setLat(point.getY());
            dto.setLon(point.getX());
        } else {
            log.warn("POI с ID {} не является точкой", entity.getId());
            throw new PoiNotPointException("POI не является Point");
        }

        dto.setStatus(entity.getStatus());
        dto.setUserGenerated(entity.isUserGenerated());

        if (entity.getSubcategories() != null) {
            List<TagDTO> tags = entity.getSubcategories().stream()
                    .map(sub -> new TagDTO(sub.getId(), sub.getWeight()))
                    .collect(Collectors.toList());
            dto.setTags(tags);
        }

        if (entity.getLocales() != null && !entity.getLocales().isEmpty()) {
            PoisLanguesEntity matchedLocale = entity.getLocales().stream()
                    .filter(l -> l.getLangue().equals(targetLang))
                    .findFirst()
                    .orElse(entity.getLocales().getFirst());

            dto.setName(matchedLocale.getPoiName());
            dto.setDescription(matchedLocale.getPoiDescription());
            dto.setLang(matchedLocale.getLangue());
        }

        if (entity.getRating() != null) {
            dto.setRate(entity.getRating().getAvgRate());
            dto.setCount(entity.getRating().getCountRate());
        } else {
            dto.setRate(0.0);
            dto.setCount(0);
        }

        return dto;
    }
}

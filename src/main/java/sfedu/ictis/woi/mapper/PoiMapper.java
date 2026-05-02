package sfedu.ictis.woi.mapper;

import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import sfedu.ictis.woi.exception.PoiNotPointException;
import sfedu.ictis.woi.model.dto.*;
import sfedu.ictis.woi.model.entity.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class PoiMapper {
    public static PoiInfoDTO mapToInfoDTO(PoiEntity entity, String targetLang) {
        PoiInfoDTO dto = new PoiInfoDTO();
        dto.setId(entity.getId());
        dto.setPoint(extractPoint(entity.getGeom(), entity.getId()));
        dto.setStatus(entity.getStatus());

        applyLocale(entity, targetLang, dto);

        if (entity.getSubcategories() != null) {
            dto.setTags(entity.getSubcategories().stream()
                    .map(PoiMapper::mapToTagNameDTO)
                    .collect(Collectors.toList()));
        }

        PoiRatingsEntity rating = entity.getRating();
        if (rating != null) {
            dto.setRating(rating.getAvgRate() != null ? rating.getAvgRate() : 0.0);
            dto.setCountRate(rating.getCountRate() != null ? rating.getCountRate() : 0);
        } else {
            dto.setRating(0.0);
            dto.setCountRate(0);
        }

        return dto;
    }

    public static PoiAdminDTO mapToAdminDTO(PoiEntity entity, String targetLang) {
        PoiAdminDTO dto = new PoiAdminDTO();
        dto.setId(entity.getId());
        dto.setPoint(extractPoint(entity.getGeom(), entity.getId()));
        dto.setStatus(entity.getStatus());
        dto.setCreatedUser(mapToUserDTO(entity.getUser()));
        dto.setLastUpdate(entity.getLastUpdate());

        applyLocale(entity, targetLang, dto);

        if (entity.getLocales() != null && !entity.getLocales().isEmpty()) {
            PoiLanguesEntity matched = getMatchedLocale(entity.getLocales(), targetLang);
            if (matched != null) {
                dto.setLang(matched.getLangue());
            }
        }

        if (entity.getSubcategories() != null) {
            dto.setTags(entity.getSubcategories().stream()
                    .map(PoiMapper::mapToTagNameDTO)
                    .collect(Collectors.toList()));
        }
        return dto;
    }

    public static PoiCardDTO mapToCardDTO(PoiEntity entity, String targetLang) {
        PoiCardDTO dto = new PoiCardDTO();
        dto.setId(entity.getId());

        PoiLanguesEntity locale = getMatchedLocale(entity.getLocales(), targetLang);
        if (locale != null) {
            dto.setName(locale.getPoiName());
        }

        if (entity.getSubcategories() != null && !entity.getSubcategories().isEmpty()) {
            SubcategoryEntity sub = entity.getSubcategories().iterator().next();
            dto.setSubcategoryName(sub.getName());
            if (sub.getCategory() != null) {
                dto.setCategoryName(sub.getCategory().getName());
            }
        }
        return dto;
    }


    private static PointDTO extractPoint(Geometry geom, Long poiId) {
        if (geom instanceof Point point) {
            return new PointDTO(point.getY(), point.getX());
        }
        log.warn("POI с ID {} не является точкой", poiId);
        throw new PoiNotPointException("POI не является Point");
    }

    private static PoiLanguesEntity getMatchedLocale(List<PoiLanguesEntity> locales, String targetLang) {
        if (locales == null || locales.isEmpty()) return null;

        for (PoiLanguesEntity l : locales) {
            if (l.getLangue().equals(targetLang)) return l;
        }

        for (PoiLanguesEntity l : locales) {
            if (l.getLangue().equals("default")) return l;
        }

        for (PoiLanguesEntity l : locales) {
            if (l.getLangue().equals("en")) return l;
        }

        return locales.getFirst();
    }

    private static void applyLocale(PoiEntity entity, String targetLang, Object dto) {
        PoiLanguesEntity locale = getMatchedLocale(entity.getLocales(), targetLang);
        if (locale != null) {
            if (dto instanceof PoiInfoDTO info) {
                info.setName(locale.getPoiName());
                info.setDescription(locale.getPoiDescription());
            } else if (dto instanceof PoiAdminDTO admin) {
                admin.setName(locale.getPoiName());
                admin.setDescription(locale.getPoiDescription());
            }
        }
    }

    private static TagNameDTO mapToTagNameDTO(SubcategoryEntity sub) {
        return new TagNameDTO(
                sub.getCategory() != null ? sub.getCategory().getId() : null,
                sub.getName(),
                new TagDTO(sub.getId(), sub.getWeight())
        );
    }

    private static UserDTO mapToUserDTO(UserEntity user) {
        if (user == null) return null;
        return new UserDTO(
                user.getId(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getCreatedAt()
        );
    }
}
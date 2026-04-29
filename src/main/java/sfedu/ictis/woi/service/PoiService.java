package sfedu.ictis.woi.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import sfedu.ictis.woi.exception.AccessDeniedException;
import sfedu.ictis.woi.exception.InvalidCredentialsException;
import sfedu.ictis.woi.exception.PoiAlreadyExistsException;
import sfedu.ictis.woi.exception.ResourceNotFoundException;
import sfedu.ictis.woi.mapper.PoiMapper;
import sfedu.ictis.woi.model.dto.*;
import sfedu.ictis.woi.model.entity.*;
import sfedu.ictis.woi.repository.PoiRepository;
import sfedu.ictis.woi.repository.SubcategoryRepository;
import sfedu.ictis.woi.repository.UserRepository;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PoiService {
    private final PoiRepository poiRepository;
    private final UserRepository userRepository;
    private final SubcategoryRepository subcategoryRepository;

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);


    public PoiInfoDTO getPoiById(Long id, String lang) {
        PoiEntity entity = poiRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("POI не найден: " + id));
        return PoiMapper.mapToInfoDTO(entity, lang);
    }

    public List<PoiCardDTO> getUserPois(Authentication authentication, String lang) {
        UserEntity user = getAuthenticatedUser(authentication);
        List<PoiEntity> userPois = poiRepository.findAllByUser(user);

        return userPois.stream()
                .map(entity -> PoiMapper.mapToCardDTO(entity, lang))
                .collect(Collectors.toList());
    }

    @Transactional
    public PoiInfoDTO createPoi(PoiAddDTO dto, Authentication authentication) {
        UserEntity user = getAuthenticatedUser(authentication);

        if (dto.getPoint() == null || dto.getPoint().getLat() == null || dto.getPoint().getLon() == null) {
            throw new IllegalArgumentException("Координаты обязательны");
        }

        if (poiRepository.existsNearby(dto.getPoint().getLon(), dto.getPoint().getLat())) {
            throw new PoiAlreadyExistsException("Точка уже существует в этой локации или слишком близко");
        }

        PoiEntity entity = new PoiEntity();
        Point point = geometryFactory.createPoint(new Coordinate(dto.getPoint().getLon(), dto.getPoint().getLat()));
        entity.setGeom(point);
        entity.setUser(user);
        entity.setStatus(PoiStatus.PENDING);

        updateSubcategories(entity, dto.getSubcategoriesId());
        addOrUpdateLocale(entity, dto.getLang(), dto.getName(), dto.getDescription());

        PoiEntity savedEntity = poiRepository.save(entity);
        return PoiMapper.mapToInfoDTO(savedEntity, dto.getLang());
    }

    @Transactional
    public PoiInfoDTO updatePoi(Long id, PoiAddDTO dto, Authentication authentication) {
        UserEntity user = getAuthenticatedUser(authentication);
        PoiEntity entity = poiRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("POI не найден: " + id));

         if (!entity.getUser().getId().equals(user.getId())) {
             throw new AccessDeniedException("Вы не можете редактировать чужую точку");
         }

        if (dto.getPoint() != null && dto.getPoint().getLat() != null && dto.getPoint().getLon() != null) {
            Point point = geometryFactory.createPoint(new Coordinate(dto.getPoint().getLon(), dto.getPoint().getLat()));
            entity.setGeom(point);
        }

        updateSubcategories(entity, dto.getSubcategoriesId());
        addOrUpdateLocale(entity, dto.getLang(), dto.getName(), dto.getDescription());

        entity.setStatus(PoiStatus.PENDING);

        PoiEntity savedEntity = poiRepository.save(entity);
        return PoiMapper.mapToInfoDTO(savedEntity, dto.getLang());
    }



    public List<PoiAdminDTO> getPoisByStatus(PoiStatus status, Pageable pageable, String targetLang) {
        Page<PoiEntity> poiPage = poiRepository.findAllByStatusIncludingDeleted(status, pageable);
        return poiPage.getContent().stream()
                .map(entity -> PoiMapper.mapToAdminDTO(entity, targetLang))
                .collect(Collectors.toList());
    }

    @Transactional
    public void updatePoiStatus(Long id, PoiStatus status) {
        PoiEntity entity = poiRepository.findByIdIncludingDeleted(id)
                .orElseThrow(() -> new ResourceNotFoundException("POI не найден: " + id));

        entity.setStatus(status);
        poiRepository.save(entity);

        log.warn("[{}] poi_id: {}",status, entity.getId());
    }



    private UserEntity getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName() == null) {
            throw new InvalidCredentialsException();
        }
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
    }

    private void updateSubcategories(PoiEntity entity, List<Integer> subcategoryIds) {
        if (subcategoryIds != null && !subcategoryIds.isEmpty()) {
            Set<SubcategoryEntity> subcategories = subcategoryIds.stream()
                    .map(id -> subcategoryRepository.findById(id)
                            .orElseThrow(() -> new ResourceNotFoundException("Подкатегория не найдена: " + id)))
                    .collect(Collectors.toSet());
            entity.setSubcategories(subcategories);
        }
    }

    private void addOrUpdateLocale(PoiEntity entity, String lang, String name, String description) {
        if (name == null && description == null) return;

        String targetLang = lang != null ? lang : "default";

        PoiLanguesEntity locale = entity.getLocales().stream()
                .filter(l -> l.getLangue().equals(targetLang))
                .findFirst()
                .orElseGet(() -> {
                    PoiLanguesEntity newLocale = new PoiLanguesEntity();
                    newLocale.setPoi(entity);
                    newLocale.setLangue(targetLang);
                    entity.getLocales().add(newLocale);
                    return newLocale;
                });

        if (name != null) locale.setPoiName(name);
        if (description != null) locale.setPoiDescription(description);
    }
}
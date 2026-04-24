package sfedu.ictis.woi.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import sfedu.ictis.woi.exception.InvalidCredentialsException;
import sfedu.ictis.woi.exception.PoiAlreadyExistsException;
import sfedu.ictis.woi.exception.ResourceNotFoundException;
import sfedu.ictis.woi.mapper.PoiMapper;
import sfedu.ictis.woi.model.dto.PoiDTO;
import sfedu.ictis.woi.model.entity.*;
import sfedu.ictis.woi.repository.PoiRepository;
import sfedu.ictis.woi.repository.SubcategoryRepository;
import sfedu.ictis.woi.repository.UserRepository;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PoiService {
    private final PoiRepository poiRepository;
    private final UserRepository userRepository;
    private final SubcategoryRepository subcategoryRepository;

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    @Transactional
    public PoiDTO createPoi(PoiDTO poiDTO, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName() == null) {
            throw new InvalidCredentialsException();
        }

        UserEntity user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));

        if (poiRepository.existsNearby(poiDTO.getLon(), poiDTO.getLat())) {
            throw new PoiAlreadyExistsException("Точка уже существует в этой локации или слишком близко (менее 5 метров)");
        }

        PoiEntity entity = new PoiEntity();

        Point point = geometryFactory.createPoint(new Coordinate(poiDTO.getLon(), poiDTO.getLat()));
        entity.setGeom(point);
        entity.setUser(user);
        entity.setStatus(PoiStatus.PENDING);

        if (poiDTO.getTags() != null && !poiDTO.getTags().isEmpty()) {
            Set<SubcategoryEntity> subcategories = poiDTO.getTags().stream()
                    .map(tag -> subcategoryRepository.findById(tag.getSubcategoryId())
                            .orElseThrow(() -> new ResourceNotFoundException("Подкатегория не найдена: " + tag.getSubcategoryId())))
                    .collect(Collectors.toSet());
            entity.setSubcategories(subcategories);
        }

        if (poiDTO.getName() != null || poiDTO.getDescription() != null) {
            PoisLanguesEntity locale = new PoisLanguesEntity();
            locale.setPoi(entity);

            locale.setLangue(poiDTO.getLang() != null ? poiDTO.getLang() : "default");
            locale.setPoiName(poiDTO.getName());
            locale.setPoiDescription(poiDTO.getDescription());

            entity.getLocales().add(locale);
        }

        PoiEntity savedEntity = poiRepository.save(entity);
        return PoiMapper.mapToDTO(savedEntity, poiDTO.getLang());
    }

    public List<PoiDTO> getPoisByStatus(PoiStatus status, Pageable pageable, String targetLang) {
        Page<PoiEntity> poiPage = poiRepository.findAllByStatus(status, pageable);
        return poiPage.getContent().stream()
                .map(entity -> PoiMapper.mapToDTO(entity, targetLang != null ? targetLang : "default"))
                .collect(Collectors.toList());
    }

    @Transactional
    public void updatePoiStatus(Long id, PoiStatus status) {
        PoiEntity entity = poiRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("POI не найден: " + id));

        if (status == PoiStatus.REJECTED) {
            poiRepository.delete(entity);
        } else {
            entity.setStatus(status);
            poiRepository.save(entity);
        }
    }
}
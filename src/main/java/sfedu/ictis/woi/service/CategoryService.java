package sfedu.ictis.woi.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import sfedu.ictis.woi.exception.ResourceNotFoundException;
import sfedu.ictis.woi.model.dto.CategoryWithSubcategoriesDTO;
import sfedu.ictis.woi.model.dto.PageResponseDTO;
import sfedu.ictis.woi.model.dto.SubcategoryShortDTO;
import sfedu.ictis.woi.model.entity.CategoryEntity;
import sfedu.ictis.woi.model.entity.SubcategoryEntity;
import sfedu.ictis.woi.repository.CategoryRepository;
import sfedu.ictis.woi.repository.SubcategoryRepository;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final SubcategoryRepository subcategoryRepository;

    public List<CategoryWithSubcategoriesDTO> getAllCategories(String lang) {
        List<CategoryEntity> categories = categoryRepository.findAllCategories();

        return categories.stream()
                .map(cat -> new CategoryWithSubcategoriesDTO(
                        cat.getId(),
                        cat.getName(),
                        cat.getIcon(),
                        Collections.emptyList()     // lazy loading
                ))
                .collect(Collectors.toList());
    }

    public PageResponseDTO<SubcategoryShortDTO> getSubcategoriesByCategory(
            Integer categoryId,
            String search,
            Pageable pageable
    ) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Категория не найдена: " + categoryId);
        }

        String normalizedSearch = (search != null && !search.isBlank()) ? search.trim() : null;
        Page<SubcategoryEntity> page = subcategoryRepository.findByCategoryWithSearch(
                categoryId, normalizedSearch, pageable
        );

        Page<SubcategoryShortDTO> dtoPage = page.map(
                s -> new SubcategoryShortDTO(s.getId(), s.getName())
        );

        return PageResponseDTO.from(dtoPage);
    }
}
package sfedu.ictis.woi.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import sfedu.ictis.woi.model.dto.CategoryWithSubcategoriesDTO;
import sfedu.ictis.woi.model.dto.SubcategoryShortDTO;
import sfedu.ictis.woi.model.entity.CategoryEntity;
import sfedu.ictis.woi.repository.CategoryRepository;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;

    public List<CategoryWithSubcategoriesDTO> getAllCategoriesWithSubcategories(String lang) { // TODO: lang - in the future
        List<CategoryEntity> categories = categoryRepository.findAllWithSubcategories();

        return categories.stream()
                .sorted(Comparator.comparing(CategoryEntity::getId))
                .map(cat -> new CategoryWithSubcategoriesDTO(
                        cat.getId(),
                        cat.getName(),
                        cat.getIcon(),
                        cat.getSubcategories().stream()
                                .sorted(Comparator.comparing(s -> s.getName() != null ? s.getName() : ""))
                                .map(s -> new SubcategoryShortDTO(s.getId(), s.getName()))
                                .collect(Collectors.toList())
                ))
                .collect(Collectors.toList());
    }
}
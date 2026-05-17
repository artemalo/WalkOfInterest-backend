package sfedu.ictis.woi.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;
import sfedu.ictis.woi.api.CategoryControllerApi;
import sfedu.ictis.woi.model.dto.CategoryPhotoDTO;
import sfedu.ictis.woi.model.dto.CategoryWithSubcategoriesDTO;
import sfedu.ictis.woi.model.dto.PageResponseDTO;
import sfedu.ictis.woi.model.dto.SubcategoryShortDTO;
import sfedu.ictis.woi.service.CategoryService;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController implements CategoryControllerApi {
    private final CategoryService categoryService;

    @GetMapping
    public List<CategoryWithSubcategoriesDTO> getAllCategories(
            @RequestParam(defaultValue = "ru") String lang
    ) {
        return categoryService.getAllCategories();
    }

    @GetMapping("/photos")
    public List<CategoryPhotoDTO> getCategoryPhotos() {
        return categoryService.getCategoryPhotos();
    }

    @GetMapping("/{id}/subcategories")
    public PageResponseDTO<SubcategoryShortDTO> getSubcategoriesByCategory(
            @PathVariable Integer id,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return categoryService.getSubcategoriesByCategory(id, search, PageRequest.of(page, size));
    }
}

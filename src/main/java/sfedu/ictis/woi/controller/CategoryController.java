package sfedu.ictis.woi.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import sfedu.ictis.woi.api.CategoryControllerApi;
import sfedu.ictis.woi.model.dto.CategoryWithSubcategoriesDTO;
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
        return categoryService.getAllCategoriesWithSubcategories(lang);
    }
}
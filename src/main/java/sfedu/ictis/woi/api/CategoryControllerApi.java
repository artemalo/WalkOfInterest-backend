package sfedu.ictis.woi.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import sfedu.ictis.woi.model.dto.CategoryWithSubcategoriesDTO;
import sfedu.ictis.woi.model.dto.PageResponseDTO;
import sfedu.ictis.woi.model.dto.SubcategoryShortDTO;

import java.util.List;

@Tag(name = "Categories", description = "Список категорий и подкатегорий")
@ApiResponses({
        @ApiResponse(responseCode = "503", description = "Проблемы с сервисом")
})
@RequestMapping("/api/categories")
public interface CategoryControllerApi {

    @Operation(
            summary = "Получить список категорий (без подкатегорий)",
            description = "Возвращает только категории. Подкатегории грузятся отдельно через /{id}/subcategories"
    )
    @GetMapping
    List<CategoryWithSubcategoriesDTO> getAllCategories(
            @Parameter(description = "Язык ответа (ru/en)")
            @RequestParam(defaultValue = "ru") String lang
    );

    @Operation(
            summary = "Подкатегории категории с пагинацией и поиском",
            description = "Возвращает страницу подкатегорий для указанной категории. " +
                    "Параметр search фильтрует по подстроке в имени (регистронезависимо)."
    )
    @GetMapping("/{id}/subcategories")
    PageResponseDTO<SubcategoryShortDTO> getSubcategoriesByCategory(
            @PathVariable Integer id,

            @Parameter(description = "Поиск по имени подкатегории")
            @RequestParam(required = false) String search,

            @Parameter(description = "Номер страницы (0-based)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Размер страницы")
            @RequestParam(defaultValue = "20") int size
    );
}

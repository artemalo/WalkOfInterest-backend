package sfedu.ictis.woi.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import sfedu.ictis.woi.model.dto.CategoryWithSubcategoriesDTO;

import java.util.List;

@Tag(name = "Categories", description = "Список категорий и подкатегорий")
@ApiResponses({
        @ApiResponse(responseCode = "503", description = "Проблемы с сервисом")
})
@RequestMapping("/api/categories")
public interface CategoryControllerApi {
    @Operation(summary = "Получить все категории с их подкатегориями")
    @GetMapping
    List<CategoryWithSubcategoriesDTO> getAllCategories(
            @Parameter(description = "Язык ответа (ru/en)")
            @RequestParam(defaultValue = "ru") String lang
    );
}
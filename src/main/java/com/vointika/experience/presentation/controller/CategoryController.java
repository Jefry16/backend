package com.vointika.experience.presentation.controller;

import com.vointika.experience.application.dto.input.CategoryInput;
import com.vointika.experience.application.usecase.CreateCategoryUseCase;
import com.vointika.experience.application.usecase.DeleteCategoryUseCase;
import com.vointika.experience.application.usecase.GetCategoryUseCase;
import com.vointika.experience.application.usecase.ListCategoriesUseCase;
import com.vointika.experience.application.usecase.UpdateCategoryUseCase;
import com.vointika.experience.domain.entity.Category;
import com.vointika.experience.presentation.response.CategoryResponse;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.ListQuery;
import com.vointika.shared.web.list.CursorPageResponse;
import com.vointika.shared.web.list.ListQueryParser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

/**
 * An operator's categories — its own classification for its experiences.
 * Membership on the operator is enforced by the {@code /api/tour-operators/**}
 * interceptor (non-member → 404); reads are member-visible, writes
 * (create/update/delete) are ADMIN+.
 */
@RestController
@RequestMapping("/api/tour-operators/{tourOperatorId}/categories")
public class CategoryController {

    private final CreateCategoryUseCase createCategoryUseCase;
    private final UpdateCategoryUseCase updateCategoryUseCase;
    private final ListCategoriesUseCase listCategoriesUseCase;
    private final GetCategoryUseCase getCategoryUseCase;
    private final DeleteCategoryUseCase deleteCategoryUseCase;
    private final ListQueryParser listQueryParser;

    public CategoryController(CreateCategoryUseCase createCategoryUseCase,
                              UpdateCategoryUseCase updateCategoryUseCase,
                              ListCategoriesUseCase listCategoriesUseCase,
                              GetCategoryUseCase getCategoryUseCase,
                              DeleteCategoryUseCase deleteCategoryUseCase,
                              ListQueryParser listQueryParser) {
        this.createCategoryUseCase = createCategoryUseCase;
        this.updateCategoryUseCase = updateCategoryUseCase;
        this.listCategoriesUseCase = listCategoriesUseCase;
        this.getCategoryUseCase = getCategoryUseCase;
        this.deleteCategoryUseCase = deleteCategoryUseCase;
        this.listQueryParser = listQueryParser;
    }

    /** The operator's categories — cursor-paginated. Any member; filter name, sort createdAt/name/id. */
    @GetMapping
    public ResponseEntity<CursorPageResponse<CategoryResponse>> list(
            @PathVariable UUID tourOperatorId,
            @AuthenticationPrincipal UUID callerUserId,
            HttpServletRequest request) {
        ListQuery query = listQueryParser.parse(request, ListCategoriesUseCase.SCHEMA, tourOperatorId);
        CursorPage<Category> page = listCategoriesUseCase.execute(query, callerUserId);
        return ResponseEntity.ok(CursorPageResponse.of(page, CategoryResponse::from));
    }

    /** A single category. Any member; 404 if not under this operator. */
    @GetMapping("/{categoryId}")
    public ResponseEntity<CategoryResponse> get(
            @PathVariable UUID tourOperatorId,
            @PathVariable UUID categoryId,
            @AuthenticationPrincipal UUID callerUserId) {
        Category category = getCategoryUseCase.execute(tourOperatorId, categoryId, callerUserId);
        return ResponseEntity.ok(CategoryResponse.from(category));
    }

    /** Creates a category. ADMIN+. 201 + Location. Duplicate name → 409. */
    @PostMapping
    public ResponseEntity<Void> create(
            @PathVariable UUID tourOperatorId,
            @RequestBody CategoryInput body,
            @AuthenticationPrincipal UUID callerUserId) {
        UUID id = createCategoryUseCase.execute(tourOperatorId, callerUserId, body);
        return ResponseEntity
                .created(URI.create("/api/tour-operators/" + tourOperatorId + "/categories/" + id))
                .build();
    }

    /** Renames a category. ADMIN+. 204. Duplicate name → 409. */
    @PatchMapping("/{categoryId}")
    public ResponseEntity<Void> update(
            @PathVariable UUID tourOperatorId,
            @PathVariable UUID categoryId,
            @RequestBody CategoryInput body,
            @AuthenticationPrincipal UUID callerUserId) {
        updateCategoryUseCase.execute(tourOperatorId, categoryId, callerUserId, body);
        return ResponseEntity.noContent().build();
    }

    /** Deletes a category. ADMIN+. 204. Its experiences survive, uncategorized. */
    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID tourOperatorId,
            @PathVariable UUID categoryId,
            @AuthenticationPrincipal UUID callerUserId) {
        deleteCategoryUseCase.execute(tourOperatorId, categoryId, callerUserId);
        return ResponseEntity.noContent().build();
    }
}

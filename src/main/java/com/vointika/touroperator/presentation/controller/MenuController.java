package com.vointika.touroperator.presentation.controller;

import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.ListQuery;
import com.vointika.shared.web.list.CursorPageResponse;
import com.vointika.shared.web.list.ListQueryParser;
import com.vointika.touroperator.application.dto.input.CreateMenuInput;
import com.vointika.touroperator.application.dto.input.RenameMenuInput;
import com.vointika.touroperator.application.dto.input.ReplaceMenuItemsInput;
import com.vointika.touroperator.application.usecase.CreateMenuUseCase;
import com.vointika.touroperator.application.usecase.DeleteMenuUseCase;
import com.vointika.touroperator.application.usecase.GetMenuUseCase;
import com.vointika.touroperator.application.usecase.ListMenusUseCase;
import com.vointika.touroperator.application.usecase.RenameMenuUseCase;
import com.vointika.touroperator.application.usecase.ReplaceMenuItemsUseCase;
import com.vointika.touroperator.domain.entity.Menu;
import com.vointika.touroperator.presentation.request.CreateMenuRequest;
import com.vointika.touroperator.presentation.request.RenameMenuRequest;
import com.vointika.touroperator.presentation.request.ReplaceMenuItemsRequest;
import com.vointika.touroperator.presentation.response.MenuDetailResponse;
import com.vointika.touroperator.presentation.response.MenuListItemResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

/**
 * The operator's storefront navigation menus. Reads member; writes ADMIN+.
 * The handle is immutable; the item tree is written wholesale via
 * {@code PUT /{menuId}/items} (no per-item CRUD).
 */
@RestController
@RequestMapping("/api/tour-operators/{tourOperatorId}/menus")
public class MenuController {

    private final CreateMenuUseCase createUseCase;
    private final RenameMenuUseCase renameUseCase;
    private final GetMenuUseCase getUseCase;
    private final ListMenusUseCase listUseCase;
    private final DeleteMenuUseCase deleteUseCase;
    private final ReplaceMenuItemsUseCase replaceItemsUseCase;
    private final ListQueryParser listQueryParser;

    public MenuController(CreateMenuUseCase createUseCase,
                          RenameMenuUseCase renameUseCase,
                          GetMenuUseCase getUseCase,
                          ListMenusUseCase listUseCase,
                          DeleteMenuUseCase deleteUseCase,
                          ReplaceMenuItemsUseCase replaceItemsUseCase,
                          ListQueryParser listQueryParser) {
        this.createUseCase = createUseCase;
        this.renameUseCase = renameUseCase;
        this.getUseCase = getUseCase;
        this.listUseCase = listUseCase;
        this.deleteUseCase = deleteUseCase;
        this.replaceItemsUseCase = replaceItemsUseCase;
        this.listQueryParser = listQueryParser;
    }

    /** Creates an empty menu (handle + title). ADMIN+. 201 + Location; duplicate handle → 409. */
    @PostMapping
    public ResponseEntity<Void> create(
            @PathVariable UUID tourOperatorId,
            @RequestBody CreateMenuRequest body,
            @AuthenticationPrincipal UUID callerUserId) {
        UUID id = createUseCase.execute(new CreateMenuInput(
                callerUserId, tourOperatorId, body.handle(), body.title()));
        return ResponseEntity
                .created(URI.create("/api/tour-operators/" + tourOperatorId + "/menus/" + id))
                .build();
    }

    /** The operator's menus (filter/sort/cursor per the list framework). Any member. */
    @GetMapping
    public ResponseEntity<CursorPageResponse<MenuListItemResponse>> list(
            @PathVariable UUID tourOperatorId,
            @AuthenticationPrincipal UUID callerUserId,
            HttpServletRequest request) {
        ListQuery query = listQueryParser.parse(request, ListMenusUseCase.SCHEMA, tourOperatorId);
        CursorPage<Menu> page = listUseCase.execute(query, callerUserId);
        return ResponseEntity.ok(CursorPageResponse.of(page, MenuListItemResponse::from));
    }

    /** One menu with its full item tree. Any member. */
    @GetMapping("/{menuId}")
    public ResponseEntity<MenuDetailResponse> get(
            @PathVariable UUID tourOperatorId,
            @PathVariable UUID menuId,
            @AuthenticationPrincipal UUID callerUserId) {
        return ResponseEntity.ok(MenuDetailResponse.from(
                getUseCase.execute(tourOperatorId, menuId, callerUserId)));
    }

    /** Renames a menu (title only — the handle is immutable). ADMIN+. */
    @PatchMapping("/{menuId}")
    public ResponseEntity<Void> rename(
            @PathVariable UUID tourOperatorId,
            @PathVariable UUID menuId,
            @RequestBody RenameMenuRequest body,
            @AuthenticationPrincipal UUID callerUserId) {
        renameUseCase.execute(new RenameMenuInput(
                callerUserId, tourOperatorId, menuId, body.title()));
        return ResponseEntity.noContent().build();
    }

    /**
     * Replaces the menu's entire item tree (the navigation editor's save).
     * ADMIN+. 422 on a tree that breaks the depth cap, a link-type rule, a
     * foreign resource id or an unsupported locale.
     */
    @PutMapping("/{menuId}/items")
    public ResponseEntity<Void> replaceItems(
            @PathVariable UUID tourOperatorId,
            @PathVariable UUID menuId,
            @RequestBody ReplaceMenuItemsRequest body,
            @AuthenticationPrincipal UUID callerUserId) {
        replaceItemsUseCase.execute(new ReplaceMenuItemsInput(
                callerUserId, tourOperatorId, menuId, body.items()));
        return ResponseEntity.noContent().build();
    }

    /** Deletes a menu and its items. ADMIN+. */
    @DeleteMapping("/{menuId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID tourOperatorId,
            @PathVariable UUID menuId,
            @AuthenticationPrincipal UUID callerUserId) {
        deleteUseCase.execute(tourOperatorId, menuId, callerUserId);
        return ResponseEntity.noContent().build();
    }
}

package com.vointika.page.presentation.controller;

import com.vointika.page.application.dto.input.CreatePageInput;
import com.vointika.page.application.dto.input.UpdatePageInput;
import com.vointika.page.application.usecase.CreatePageUseCase;
import com.vointika.page.application.usecase.DeletePageUseCase;
import com.vointika.page.application.usecase.GetPageUseCase;
import com.vointika.page.application.usecase.ListPagesUseCase;
import com.vointika.page.application.usecase.PublishPageUseCase;
import com.vointika.page.application.usecase.RenamePageUseCase;
import com.vointika.page.application.usecase.UnpublishPageUseCase;
import com.vointika.page.application.usecase.UpdatePageUseCase;
import com.vointika.page.domain.projection.PageListItem;
import com.vointika.page.presentation.request.CreatePageRequest;
import com.vointika.page.presentation.request.RenamePageRequest;
import com.vointika.page.presentation.request.UpdatePageRequest;
import com.vointika.page.presentation.response.PageListItemResponse;
import com.vointika.page.presentation.response.PageResponse;
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
 * Admin CRUD for CMS content pages. Membership enforced by the
 * {@code /api/tour-operators/**} interceptor plus the use cases' role gates
 * (reads member, writes ADMIN+); page ownership by
 * {@code findByIdAndTourOperatorId} (byte-identical 404).
 */
@RestController
@RequestMapping("/api/tour-operators/{tourOperatorId}/pages")
public class PageController {

    private final CreatePageUseCase createPageUseCase;
    private final UpdatePageUseCase updatePageUseCase;
    private final ListPagesUseCase listPagesUseCase;
    private final GetPageUseCase getPageUseCase;
    private final PublishPageUseCase publishPageUseCase;
    private final UnpublishPageUseCase unpublishPageUseCase;
    private final RenamePageUseCase renamePageUseCase;
    private final DeletePageUseCase deletePageUseCase;
    private final ListQueryParser listQueryParser;

    public PageController(CreatePageUseCase createPageUseCase,
                          UpdatePageUseCase updatePageUseCase,
                          ListPagesUseCase listPagesUseCase,
                          GetPageUseCase getPageUseCase,
                          PublishPageUseCase publishPageUseCase,
                          UnpublishPageUseCase unpublishPageUseCase,
                          RenamePageUseCase renamePageUseCase,
                          DeletePageUseCase deletePageUseCase,
                          ListQueryParser listQueryParser) {
        this.createPageUseCase = createPageUseCase;
        this.updatePageUseCase = updatePageUseCase;
        this.listPagesUseCase = listPagesUseCase;
        this.getPageUseCase = getPageUseCase;
        this.publishPageUseCase = publishPageUseCase;
        this.unpublishPageUseCase = unpublishPageUseCase;
        this.renamePageUseCase = renamePageUseCase;
        this.deletePageUseCase = deletePageUseCase;
        this.listQueryParser = listQueryParser;
    }

    /** The operator's pages, bodies excluded. Any member. */
    @GetMapping
    public ResponseEntity<CursorPageResponse<PageListItemResponse>> list(
            @PathVariable UUID tourOperatorId,
            @AuthenticationPrincipal UUID callerUserId,
            HttpServletRequest request) {
        ListQuery query = listQueryParser.parse(request, ListPagesUseCase.SCHEMA, tourOperatorId);
        CursorPage<PageListItem> page = listPagesUseCase.execute(query, callerUserId);
        return ResponseEntity.ok(CursorPageResponse.of(page, PageListItemResponse::from));
    }

    /** A single page, body included. Any member. */
    @GetMapping("/{pageId}")
    public ResponseEntity<PageResponse> get(
            @PathVariable UUID tourOperatorId,
            @PathVariable UUID pageId,
            @AuthenticationPrincipal UUID callerUserId) {
        return ResponseEntity.ok(PageResponse.from(
                getPageUseCase.execute(tourOperatorId, pageId, callerUserId)));
    }

    /** Creates a DRAFT page. ADMIN+. 201 + Location; duplicate handle → 409. */
    @PostMapping
    public ResponseEntity<Void> create(
            @PathVariable UUID tourOperatorId,
            @RequestBody CreatePageRequest body,
            @AuthenticationPrincipal UUID callerUserId) {
        UUID id = createPageUseCase.execute(new CreatePageInput(
                callerUserId, tourOperatorId,
                body.title(), body.handle(), body.body(),
                body.seoTitle(), body.seoDescription()));
        return ResponseEntity
                .created(URI.create("/api/tour-operators/" + tourOperatorId + "/pages/" + id))
                .build();
    }

    /** Whole replace of the editable content (handle/status untouched). ADMIN+. */
    @PatchMapping("/{pageId}")
    public ResponseEntity<Void> update(
            @PathVariable UUID tourOperatorId,
            @PathVariable UUID pageId,
            @RequestBody UpdatePageRequest body,
            @AuthenticationPrincipal UUID callerUserId) {
        updatePageUseCase.execute(new UpdatePageInput(
                callerUserId, tourOperatorId, pageId,
                body.title(), body.body(), body.seoTitle(), body.seoDescription()));
        return ResponseEntity.noContent().build();
    }

    /** DRAFT → PUBLISHED. ADMIN+; already published → 409. */
    @PostMapping("/{pageId}/publish")
    public ResponseEntity<Void> publish(
            @PathVariable UUID tourOperatorId,
            @PathVariable UUID pageId,
            @AuthenticationPrincipal UUID callerUserId) {
        publishPageUseCase.execute(tourOperatorId, pageId, callerUserId);
        return ResponseEntity.noContent().build();
    }

    /** PUBLISHED → DRAFT. ADMIN+; already a draft → 409. */
    @PostMapping("/{pageId}/unpublish")
    public ResponseEntity<Void> unpublish(
            @PathVariable UUID tourOperatorId,
            @PathVariable UUID pageId,
            @AuthenticationPrincipal UUID callerUserId) {
        unpublishPageUseCase.execute(tourOperatorId, pageId, callerUserId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Renames the canonical handle — a dedicated endpoint (not part of the
     * content PATCH): changing the page's permanent URL is a deliberate act.
     * ADMIN+; taken handle → 409.
     */
    @PostMapping("/{pageId}/rename")
    public ResponseEntity<Void> rename(
            @PathVariable UUID tourOperatorId,
            @PathVariable UUID pageId,
            @RequestBody RenamePageRequest body,
            @AuthenticationPrincipal UUID callerUserId) {
        renamePageUseCase.execute(tourOperatorId, pageId, body.handle(),
                callerUserId);
        return ResponseEntity.noContent().build();
    }

    /** Deletes a page (translations cascade). ADMIN+. */
    @DeleteMapping("/{pageId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID tourOperatorId,
            @PathVariable UUID pageId,
            @AuthenticationPrincipal UUID callerUserId) {
        deletePageUseCase.execute(tourOperatorId, pageId, callerUserId);
        return ResponseEntity.noContent().build();
    }
}

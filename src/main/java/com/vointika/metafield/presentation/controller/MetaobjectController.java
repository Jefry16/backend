package com.vointika.metafield.presentation.controller;

import com.vointika.metafield.application.dto.input.CreateMetaobjectEntryInput;
import com.vointika.metafield.application.dto.input.UpdateMetaobjectEntryInput;
import com.vointika.metafield.application.usecase.CreateMetaobjectEntryUseCase;
import com.vointika.metafield.application.usecase.DeleteMetaobjectEntryUseCase;
import com.vointika.metafield.application.usecase.GetMetaobjectEntryUseCase;
import com.vointika.metafield.application.usecase.ListMetaobjectEntriesUseCase;
import com.vointika.metafield.application.usecase.PublishMetaobjectEntryUseCase;
import com.vointika.metafield.application.usecase.UnpublishMetaobjectEntryUseCase;
import com.vointika.metafield.application.usecase.UpdateMetaobjectEntryUseCase;
import com.vointika.metafield.domain.projection.MetaobjectEntryListItem;
import com.vointika.metafield.presentation.request.CreateMetaobjectRequest;
import com.vointika.metafield.presentation.request.UpdateMetaobjectRequest;
import com.vointika.metafield.presentation.response.MetaobjectListItemResponse;
import com.vointika.metafield.presentation.response.MetaobjectResponse;
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
 * The operator's metaobject entries (the content of the definitions). Reads
 * member; writes ADMIN+. The per-type view is the list filtered by
 * {@code definitionId}.
 */
@RestController
@RequestMapping("/api/tour-operators/{tourOperatorId}/metaobjects")
public class MetaobjectController {

    private final CreateMetaobjectEntryUseCase createUseCase;
    private final UpdateMetaobjectEntryUseCase updateUseCase;
    private final GetMetaobjectEntryUseCase getUseCase;
    private final ListMetaobjectEntriesUseCase listUseCase;
    private final DeleteMetaobjectEntryUseCase deleteUseCase;
    private final PublishMetaobjectEntryUseCase publishUseCase;
    private final UnpublishMetaobjectEntryUseCase unpublishUseCase;
    private final ListQueryParser listQueryParser;

    public MetaobjectController(CreateMetaobjectEntryUseCase createUseCase,
                                UpdateMetaobjectEntryUseCase updateUseCase,
                                GetMetaobjectEntryUseCase getUseCase,
                                ListMetaobjectEntriesUseCase listUseCase,
                                DeleteMetaobjectEntryUseCase deleteUseCase,
                                PublishMetaobjectEntryUseCase publishUseCase,
                                UnpublishMetaobjectEntryUseCase unpublishUseCase,
                                ListQueryParser listQueryParser) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.getUseCase = getUseCase;
        this.listUseCase = listUseCase;
        this.deleteUseCase = deleteUseCase;
        this.publishUseCase = publishUseCase;
        this.unpublishUseCase = unpublishUseCase;
        this.listQueryParser = listQueryParser;
    }

    /** Creates an entry (always unpublished). ADMIN+. 201 + Location; duplicate handle → 409. */
    @PostMapping
    public ResponseEntity<Void> create(
            @PathVariable UUID tourOperatorId,
            @RequestBody CreateMetaobjectRequest body,
            @AuthenticationPrincipal UUID callerUserId) {
        UUID id = createUseCase.execute(new CreateMetaobjectEntryInput(
                callerUserId, tourOperatorId,
                body.definitionId(), body.handle(), body.name(), body.values()));
        return ResponseEntity
                .created(URI.create("/api/tour-operators/" + tourOperatorId
                        + "/metaobjects/" + id))
                .build();
    }

    /** The operator's entries; filter by definitionId for the per-type view. Any member. */
    @GetMapping
    public ResponseEntity<CursorPageResponse<MetaobjectListItemResponse>> list(
            @PathVariable UUID tourOperatorId,
            @AuthenticationPrincipal UUID callerUserId,
            HttpServletRequest request) {
        ListQuery query = listQueryParser.parse(
                request, ListMetaobjectEntriesUseCase.SCHEMA, tourOperatorId);
        CursorPage<MetaobjectEntryListItem> page =
                listUseCase.execute(query, callerUserId);
        return ResponseEntity.ok(
                CursorPageResponse.of(page, MetaobjectListItemResponse::from));
    }

    /** A single entry with every field (value null when unset). Any member. */
    @GetMapping("/{metaobjectId}")
    public ResponseEntity<MetaobjectResponse> get(
            @PathVariable UUID tourOperatorId,
            @PathVariable UUID metaobjectId,
            @AuthenticationPrincipal UUID callerUserId) {
        return ResponseEntity.ok(MetaobjectResponse.from(
                getUseCase.execute(tourOperatorId, metaobjectId, callerUserId)));
    }

    /** PATCHes name/handle and/or any subset of field values. ADMIN+. */
    @PatchMapping("/{metaobjectId}")
    public ResponseEntity<Void> update(
            @PathVariable UUID tourOperatorId,
            @PathVariable UUID metaobjectId,
            @RequestBody UpdateMetaobjectRequest body,
            @AuthenticationPrincipal UUID callerUserId) {
        updateUseCase.execute(new UpdateMetaobjectEntryInput(
                callerUserId, tourOperatorId, metaobjectId,
                body.name(), body.handle(), body.values()));
        return ResponseEntity.noContent().build();
    }

    /** Publishes. ADMIN+. Already published → 409. */
    @PostMapping("/{metaobjectId}/publish")
    public ResponseEntity<Void> publish(
            @PathVariable UUID tourOperatorId,
            @PathVariable UUID metaobjectId,
            @AuthenticationPrincipal UUID callerUserId) {
        publishUseCase.execute(tourOperatorId, metaobjectId, callerUserId);
        return ResponseEntity.noContent().build();
    }

    /** Unpublishes. ADMIN+. Not published → 409. */
    @PostMapping("/{metaobjectId}/unpublish")
    public ResponseEntity<Void> unpublish(
            @PathVariable UUID tourOperatorId,
            @PathVariable UUID metaobjectId,
            @AuthenticationPrincipal UUID callerUserId) {
        unpublishUseCase.execute(tourOperatorId, metaobjectId, callerUserId);
        return ResponseEntity.noContent().build();
    }

    /** Deletes an entry (values cascade). ADMIN+. */
    @DeleteMapping("/{metaobjectId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID tourOperatorId,
            @PathVariable UUID metaobjectId,
            @AuthenticationPrincipal UUID callerUserId) {
        deleteUseCase.execute(tourOperatorId, metaobjectId, callerUserId);
        return ResponseEntity.noContent().build();
    }
}

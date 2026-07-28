package com.vointika.metafield.presentation.controller;

import com.vointika.metafield.application.dto.input.AddMetaobjectFieldInput;
import com.vointika.metafield.application.dto.input.CreateMetaobjectDefinitionInput;
import com.vointika.metafield.application.dto.input.RenameMetaobjectFieldInput;
import com.vointika.metafield.application.dto.input.UpdateMetaobjectDefinitionInput;
import com.vointika.metafield.application.usecase.AddMetaobjectFieldUseCase;
import com.vointika.metafield.application.usecase.CreateMetaobjectDefinitionUseCase;
import com.vointika.metafield.application.usecase.DeleteMetaobjectDefinitionUseCase;
import com.vointika.metafield.application.usecase.GetMetaobjectDefinitionUseCase;
import com.vointika.metafield.application.usecase.ListMetaobjectDefinitionsUseCase;
import com.vointika.metafield.application.usecase.RemoveMetaobjectFieldUseCase;
import com.vointika.metafield.application.usecase.RenameMetaobjectFieldUseCase;
import com.vointika.metafield.application.usecase.UpdateMetaobjectDefinitionUseCase;
import com.vointika.metafield.domain.projection.MetaobjectDefinitionListItem;
import com.vointika.metafield.presentation.request.AddMetaobjectFieldRequest;
import com.vointika.metafield.presentation.request.CreateMetaobjectDefinitionRequest;
import com.vointika.metafield.presentation.request.RenameMetaobjectFieldRequest;
import com.vointika.metafield.presentation.request.UpdateMetaobjectDefinitionRequest;
import com.vointika.metafield.presentation.response.MetaobjectDefinitionListItemResponse;
import com.vointika.metafield.presentation.response.MetaobjectDefinitionResponse;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * Admin CRUD for metaobject definitions and their field sets. Membership
 * enforced by the interceptor plus the use cases' role gates (reads member,
 * writes ADMIN+).
 */
@RestController
@RequestMapping("/api/tour-operators/{tourOperatorId}/metaobject-definitions")
public class MetaobjectDefinitionController {

    private final CreateMetaobjectDefinitionUseCase createUseCase;
    private final UpdateMetaobjectDefinitionUseCase updateUseCase;
    private final GetMetaobjectDefinitionUseCase getUseCase;
    private final ListMetaobjectDefinitionsUseCase listUseCase;
    private final DeleteMetaobjectDefinitionUseCase deleteUseCase;
    private final AddMetaobjectFieldUseCase addFieldUseCase;
    private final RenameMetaobjectFieldUseCase renameFieldUseCase;
    private final RemoveMetaobjectFieldUseCase removeFieldUseCase;
    private final ListQueryParser listQueryParser;

    public MetaobjectDefinitionController(CreateMetaobjectDefinitionUseCase createUseCase,
                                          UpdateMetaobjectDefinitionUseCase updateUseCase,
                                          GetMetaobjectDefinitionUseCase getUseCase,
                                          ListMetaobjectDefinitionsUseCase listUseCase,
                                          DeleteMetaobjectDefinitionUseCase deleteUseCase,
                                          AddMetaobjectFieldUseCase addFieldUseCase,
                                          RenameMetaobjectFieldUseCase renameFieldUseCase,
                                          RemoveMetaobjectFieldUseCase removeFieldUseCase,
                                          ListQueryParser listQueryParser) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.getUseCase = getUseCase;
        this.listUseCase = listUseCase;
        this.deleteUseCase = deleteUseCase;
        this.addFieldUseCase = addFieldUseCase;
        this.renameFieldUseCase = renameFieldUseCase;
        this.removeFieldUseCase = removeFieldUseCase;
        this.listQueryParser = listQueryParser;
    }

    /** Creates a definition WITH its initial fields. ADMIN+. 201 + Location; duplicate type → 409. */
    @PostMapping
    public ResponseEntity<Void> create(
            @PathVariable UUID tourOperatorId,
            @RequestBody CreateMetaobjectDefinitionRequest body,
            @AuthenticationPrincipal String callerUserId) {
        List<CreateMetaobjectDefinitionInput.FieldSpec> fields = body.fields() == null
                ? List.of()
                : body.fields().stream()
                        .map(f -> new CreateMetaobjectDefinitionInput.FieldSpec(
                                f.key(), f.type(), f.name()))
                        .toList();
        UUID id = createUseCase.execute(new CreateMetaobjectDefinitionInput(
                UUID.fromString(callerUserId), tourOperatorId,
                body.type(), body.name(), body.description(), fields));
        return ResponseEntity
                .created(URI.create("/api/tour-operators/" + tourOperatorId
                        + "/metaobject-definitions/" + id))
                .build();
    }

    /** The operator's definitions. Any member. */
    @GetMapping
    public ResponseEntity<CursorPageResponse<MetaobjectDefinitionListItemResponse>> list(
            @PathVariable UUID tourOperatorId,
            @AuthenticationPrincipal String callerUserId,
            HttpServletRequest request) {
        ListQuery query = listQueryParser.parse(
                request, ListMetaobjectDefinitionsUseCase.SCHEMA, tourOperatorId);
        CursorPage<MetaobjectDefinitionListItem> page =
                listUseCase.execute(query, UUID.fromString(callerUserId));
        return ResponseEntity.ok(
                CursorPageResponse.of(page, MetaobjectDefinitionListItemResponse::from));
    }

    /** A single definition with its ordered fields. Any member; cross-tenant → 404. */
    @GetMapping("/{definitionId}")
    public ResponseEntity<MetaobjectDefinitionResponse> get(
            @PathVariable UUID tourOperatorId,
            @PathVariable UUID definitionId,
            @AuthenticationPrincipal String callerUserId) {
        return ResponseEntity.ok(MetaobjectDefinitionResponse.from(
                getUseCase.execute(tourOperatorId, definitionId, UUID.fromString(callerUserId))));
    }

    /** Updates name/description ({@code type} is immutable). ADMIN+. */
    @PutMapping("/{definitionId}")
    public ResponseEntity<Void> update(
            @PathVariable UUID tourOperatorId,
            @PathVariable UUID definitionId,
            @RequestBody UpdateMetaobjectDefinitionRequest body,
            @AuthenticationPrincipal String callerUserId) {
        updateUseCase.execute(new UpdateMetaobjectDefinitionInput(
                UUID.fromString(callerUserId), tourOperatorId, definitionId,
                body.name(), body.description()));
        return ResponseEntity.noContent().build();
    }

    /** Deletes a definition — CASCADES its entries and every stored value. ADMIN+. */
    @DeleteMapping("/{definitionId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID tourOperatorId,
            @PathVariable UUID definitionId,
            @AuthenticationPrincipal String callerUserId) {
        deleteUseCase.execute(tourOperatorId, definitionId, UUID.fromString(callerUserId));
        return ResponseEntity.noContent().build();
    }

    /** Appends a field. ADMIN+. Duplicate key → 409. */
    @PostMapping("/{definitionId}/fields")
    public ResponseEntity<Void> addField(
            @PathVariable UUID tourOperatorId,
            @PathVariable UUID definitionId,
            @RequestBody AddMetaobjectFieldRequest body,
            @AuthenticationPrincipal String callerUserId) {
        addFieldUseCase.execute(new AddMetaobjectFieldInput(
                UUID.fromString(callerUserId), tourOperatorId, definitionId,
                body.key(), body.type(), body.name()));
        return ResponseEntity.noContent().build();
    }

    /** Renames a field's display name (key/type immutable). ADMIN+. */
    @PatchMapping("/{definitionId}/fields/{key}")
    public ResponseEntity<Void> renameField(
            @PathVariable UUID tourOperatorId,
            @PathVariable UUID definitionId,
            @PathVariable String key,
            @RequestBody RenameMetaobjectFieldRequest body,
            @AuthenticationPrincipal String callerUserId) {
        renameFieldUseCase.execute(new RenameMetaobjectFieldInput(
                UUID.fromString(callerUserId), tourOperatorId, definitionId, key, body.name()));
        return ResponseEntity.noContent().build();
    }

    /** Removes a field — CASCADES its stored values. ADMIN+. Last field → 409. */
    @DeleteMapping("/{definitionId}/fields/{key}")
    public ResponseEntity<Void> removeField(
            @PathVariable UUID tourOperatorId,
            @PathVariable UUID definitionId,
            @PathVariable String key,
            @AuthenticationPrincipal String callerUserId) {
        removeFieldUseCase.execute(tourOperatorId, definitionId, key, UUID.fromString(callerUserId));
        return ResponseEntity.noContent().build();
    }
}

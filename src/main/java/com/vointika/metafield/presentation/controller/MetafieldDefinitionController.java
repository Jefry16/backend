package com.vointika.metafield.presentation.controller;

import com.vointika.metafield.application.dto.input.CreateMetafieldDefinitionInput;
import com.vointika.metafield.application.dto.input.UpdateMetafieldDefinitionInput;
import com.vointika.metafield.application.usecase.CreateMetafieldDefinitionUseCase;
import com.vointika.metafield.application.usecase.DeleteMetafieldDefinitionUseCase;
import com.vointika.metafield.application.usecase.GetMetafieldDefinitionUseCase;
import com.vointika.metafield.application.usecase.ListMetafieldDefinitionsUseCase;
import com.vointika.metafield.application.usecase.UpdateMetafieldDefinitionUseCase;
import com.vointika.metafield.domain.projection.MetafieldDefinitionListItem;
import com.vointika.metafield.presentation.request.CreateMetafieldDefinitionRequest;
import com.vointika.metafield.presentation.request.UpdateMetafieldDefinitionRequest;
import com.vointika.metafield.presentation.response.MetafieldDefinitionListItemResponse;
import com.vointika.metafield.presentation.response.MetafieldDefinitionResponse;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.ListQuery;
import com.vointika.shared.web.list.CursorPageResponse;
import com.vointika.shared.web.list.ListQueryParser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

/**
 * Admin CRUD for the definition catalogue. Membership enforced by the
 * interceptor plus the use cases' role gates (reads member, writes ADMIN+).
 */
@RestController
@RequestMapping("/api/tour-operators/{tourOperatorId}/metafield-definitions")
public class MetafieldDefinitionController {

    private final CreateMetafieldDefinitionUseCase createUseCase;
    private final UpdateMetafieldDefinitionUseCase updateUseCase;
    private final GetMetafieldDefinitionUseCase getUseCase;
    private final ListMetafieldDefinitionsUseCase listUseCase;
    private final DeleteMetafieldDefinitionUseCase deleteUseCase;
    private final ListQueryParser listQueryParser;

    public MetafieldDefinitionController(CreateMetafieldDefinitionUseCase createUseCase,
                                         UpdateMetafieldDefinitionUseCase updateUseCase,
                                         GetMetafieldDefinitionUseCase getUseCase,
                                         ListMetafieldDefinitionsUseCase listUseCase,
                                         DeleteMetafieldDefinitionUseCase deleteUseCase,
                                         ListQueryParser listQueryParser) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.getUseCase = getUseCase;
        this.listUseCase = listUseCase;
        this.deleteUseCase = deleteUseCase;
        this.listQueryParser = listQueryParser;
    }

    /** Creates a definition. ADMIN+. 201 + Location; duplicate namespace.key → 409. */
    @PostMapping
    public ResponseEntity<Void> create(
            @PathVariable UUID tourOperatorId,
            @RequestBody CreateMetafieldDefinitionRequest body,
            @AuthenticationPrincipal UUID callerUserId) {
        UUID id = createUseCase.execute(new CreateMetafieldDefinitionInput(
                callerUserId, tourOperatorId,
                body.ownerType(), body.namespace(), body.key(), body.type(),
                body.metaobjectDefinitionId(), body.name(), body.description()));
        return ResponseEntity
                .created(URI.create("/api/tour-operators/" + tourOperatorId
                        + "/metafield-definitions/" + id))
                .build();
    }

    /** The operator's definitions. Any member. */
    @GetMapping
    public ResponseEntity<CursorPageResponse<MetafieldDefinitionListItemResponse>> list(
            @PathVariable UUID tourOperatorId,
            @AuthenticationPrincipal UUID callerUserId,
            HttpServletRequest request) {
        ListQuery query = listQueryParser.parse(
                request, ListMetafieldDefinitionsUseCase.SCHEMA, tourOperatorId);
        CursorPage<MetafieldDefinitionListItem> page =
                listUseCase.execute(query, callerUserId);
        return ResponseEntity.ok(
                CursorPageResponse.of(page, MetafieldDefinitionListItemResponse::from));
    }

    /** A single definition. Any member; cross-tenant → 404. */
    @GetMapping("/{definitionId}")
    public ResponseEntity<MetafieldDefinitionResponse> get(
            @PathVariable UUID tourOperatorId,
            @PathVariable UUID definitionId,
            @AuthenticationPrincipal UUID callerUserId) {
        return ResponseEntity.ok(MetafieldDefinitionResponse.from(
                getUseCase.execute(tourOperatorId, definitionId, callerUserId)));
    }

    /** Updates name/description (the identity fields are immutable). ADMIN+. */
    @PutMapping("/{definitionId}")
    public ResponseEntity<Void> update(
            @PathVariable UUID tourOperatorId,
            @PathVariable UUID definitionId,
            @RequestBody UpdateMetafieldDefinitionRequest body,
            @AuthenticationPrincipal UUID callerUserId) {
        updateUseCase.execute(new UpdateMetafieldDefinitionInput(
                callerUserId, tourOperatorId, definitionId,
                body.name(), body.description()));
        return ResponseEntity.noContent().build();
    }

    /** Deletes a definition — CASCADES every stored value for it. ADMIN+. */
    @DeleteMapping("/{definitionId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID tourOperatorId,
            @PathVariable UUID definitionId,
            @AuthenticationPrincipal UUID callerUserId) {
        deleteUseCase.execute(tourOperatorId, definitionId, callerUserId);
        return ResponseEntity.noContent().build();
    }
}

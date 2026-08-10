package com.vointika.experience.presentation.controller;

import com.vointika.experience.application.dto.input.ExperienceInput;
import com.vointika.experience.application.dto.output.ExperienceView;
import com.vointika.experience.application.usecase.CreateExperienceUseCase;
import com.vointika.experience.application.usecase.GetExperienceUseCase;
import com.vointika.experience.application.usecase.ListExperiencesUseCase;
import com.vointika.experience.application.usecase.PublishExperienceUseCase;
import com.vointika.experience.application.usecase.UnpublishExperienceUseCase;
import com.vointika.experience.application.usecase.UpdateExperienceUseCase;
import com.vointika.experience.presentation.request.ExperienceRequest;
import com.vointika.experience.presentation.response.ExperienceResponse;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.ListQuery;
import com.vointika.shared.web.list.CursorPageResponse;
import com.vointika.shared.web.list.ListQueryParser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
 * An operator's experiences (its catalog). Membership on the operator is
 * enforced by the {@code /api/tour-operators/**} interceptor (non-member → 404);
 * reads are member-visible, writes (create/update/publish) are ADMIN+.
 */
@RestController
@RequestMapping("/api/tour-operators/{tourOperatorId}/experiences")
public class ExperienceController {

    private final CreateExperienceUseCase createExperienceUseCase;
    private final ListExperiencesUseCase listExperiencesUseCase;
    private final GetExperienceUseCase getExperienceUseCase;
    private final UpdateExperienceUseCase updateExperienceUseCase;
    private final PublishExperienceUseCase publishExperienceUseCase;
    private final UnpublishExperienceUseCase unpublishExperienceUseCase;
    private final ListQueryParser listQueryParser;

    public ExperienceController(CreateExperienceUseCase createExperienceUseCase,
                                ListExperiencesUseCase listExperiencesUseCase,
                                GetExperienceUseCase getExperienceUseCase,
                                UpdateExperienceUseCase updateExperienceUseCase,
                                PublishExperienceUseCase publishExperienceUseCase,
                                UnpublishExperienceUseCase unpublishExperienceUseCase,
                                ListQueryParser listQueryParser) {
        this.createExperienceUseCase = createExperienceUseCase;
        this.listExperiencesUseCase = listExperiencesUseCase;
        this.getExperienceUseCase = getExperienceUseCase;
        this.updateExperienceUseCase = updateExperienceUseCase;
        this.publishExperienceUseCase = publishExperienceUseCase;
        this.unpublishExperienceUseCase = unpublishExperienceUseCase;
        this.listQueryParser = listQueryParser;
    }

    /** The operator's experiences — cursor-paginated. Any member; filter status/featured, sort createdAt/name/id. */
    @GetMapping
    public ResponseEntity<CursorPageResponse<ExperienceResponse>> list(
            @PathVariable UUID tourOperatorId,
            @AuthenticationPrincipal String callerUserId,
            HttpServletRequest request) {
        ListQuery query = listQueryParser.parse(request, ListExperiencesUseCase.SCHEMA, tourOperatorId);
        CursorPage<ExperienceView> page = listExperiencesUseCase.execute(query, UUID.fromString(callerUserId));
        return ResponseEntity.ok(CursorPageResponse.of(page, ExperienceResponse::from));
    }

    /** A single experience (media resolved). Any member; 404 if not under this operator. */
    @GetMapping("/{experienceId}")
    public ResponseEntity<ExperienceResponse> get(
            @PathVariable UUID tourOperatorId,
            @PathVariable UUID experienceId,
            @AuthenticationPrincipal String userIdStr) {
        ExperienceView view = getExperienceUseCase.execute(tourOperatorId, experienceId, UUID.fromString(userIdStr));
        return ResponseEntity.ok(ExperienceResponse.from(view));
    }

    /** Creates a DRAFT experience. ADMIN+. 201 + Location. */
    @PostMapping
    public ResponseEntity<Void> create(
            @PathVariable UUID tourOperatorId,
            @RequestBody ExperienceRequest body,
            @AuthenticationPrincipal String userIdStr) {
        UUID id = createExperienceUseCase.execute(tourOperatorId, UUID.fromString(userIdStr), toInput(body));
        return ResponseEntity
                .created(URI.create("/api/tour-operators/" + tourOperatorId + "/experiences/" + id))
                .build();
    }

    /** Updates an experience's editable fields. ADMIN+. 204. */
    @PatchMapping("/{experienceId}")
    public ResponseEntity<Void> update(
            @PathVariable UUID tourOperatorId,
            @PathVariable UUID experienceId,
            @RequestBody ExperienceRequest body,
            @AuthenticationPrincipal String userIdStr) {
        updateExperienceUseCase.execute(tourOperatorId, experienceId, UUID.fromString(userIdStr), toInput(body));
        return ResponseEntity.noContent().build();
    }

    /** Publishes (DRAFT → PUBLISHED). ADMIN+. 204. */
    @PostMapping("/{experienceId}/publish")
    public ResponseEntity<Void> publish(
            @PathVariable UUID tourOperatorId,
            @PathVariable UUID experienceId,
            @AuthenticationPrincipal String userIdStr) {
        publishExperienceUseCase.execute(tourOperatorId, experienceId, UUID.fromString(userIdStr));
        return ResponseEntity.noContent().build();
    }

    /** Unpublishes (PUBLISHED → DRAFT). ADMIN+. 204. */
    @PostMapping("/{experienceId}/unpublish")
    public ResponseEntity<Void> unpublish(
            @PathVariable UUID tourOperatorId,
            @PathVariable UUID experienceId,
            @AuthenticationPrincipal String userIdStr) {
        unpublishExperienceUseCase.execute(tourOperatorId, experienceId, UUID.fromString(userIdStr));
        return ResponseEntity.noContent().build();
    }

    private static ExperienceInput toInput(ExperienceRequest b) {
        return new ExperienceInput(
                b.name(), b.description(), b.longDescription(),
                Boolean.TRUE.equals(b.featured()),
                b.mediaIds(), b.thumbnailMediaId(), b.bookingCutoffHours(),
                b.seoTitle(), b.seoDescription(), b.startingPrice());
    }
}

package com.vointika.audit.presentation.controller;

import com.vointika.shared.web.docs.ApiErrorSnippets;
import com.vointika.audit.application.usecase.GetAuditLogEntryUseCase;
import com.vointika.audit.application.usecase.ListAuditLogUseCase;
import com.vointika.audit.domain.projection.AuditLogListItem;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.port.AccessTokenValidatorPort;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.valueobject.AuditActorType;
import com.vointika.shared.web.list.ListQueryParser;
import com.vointika.shared.web.security.SecurityConfig;
import com.vointika.touroperator.infrastructure.web.WebConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.restdocs.payload.JsonFieldType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;

import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuditLogController.class)
@ExtendWith(RestDocumentationExtension.class)
@Import({SecurityConfig.class, WebConfig.class, ListQueryParser.class})
class AuditLogControllerDocumentationTest {

    private static final String OP = "019f7f33-1833-7dc1-b008-47e6c68b3ea2";
    private static final String ENTRY = "019f7f33-2222-7dc1-b008-47e6c68b3ea2";
    private static final String USER = "550e8400-e29b-41d4-a716-446655440000";
    private static final String TOKEN = "test-access-token";
    private static final String BEARER = "Bearer " + TOKEN;

    /** An id no entry has, so the 404 example is not the 200 example. */
    private static final String MISSING_ENTRY = "cccccccc-0000-4000-8000-000000000404";

    /**
     * A STAFF member's token. A 403 turns on <em>who</em> is asking rather than what
     * is asked for, so the request URL is necessarily the one that succeeds for an
     * ADMIN — varying the token is what makes the published example legible.
     * {@code PublishedExamplesAreHonestTest} fails the build without it.
     */
    private static final String STAFF_TOKEN = "staff-access-token";
    private static final String STAFF_BEARER = "Bearer " + STAFF_TOKEN;
    private static final String STAFF_USER = "550e8400-e29b-41d4-a716-4466554400ff";

    private MockMvc mockMvc;

    @MockitoBean private ListAuditLogUseCase listAuditLogUseCase;
    @MockitoBean private GetAuditLogEntryUseCase getAuditLogEntryUseCase;
    @MockitoBean private TourOperatorMembershipCheck membershipCheck;
    @MockitoBean private AccessTokenValidatorPort accessTokenValidator;

    @BeforeEach
    void setUp(WebApplicationContext context, RestDocumentationContextProvider restDocumentation) {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(documentationConfiguration(restDocumentation)
                        .operationPreprocessors()
                        .withRequestDefaults(prettyPrint())
                        .withResponseDefaults(prettyPrint()))
                .apply(springSecurity())
                .build();
    }

    private void authenticated() {
        when(accessTokenValidator.isValid(TOKEN)).thenReturn(true);
        when(accessTokenValidator.extractUserId(TOKEN)).thenReturn(USER);
    }

    private void authenticatedAsStaff() {
        when(accessTokenValidator.isValid(STAFF_TOKEN)).thenReturn(true);
        when(accessTokenValidator.extractUserId(STAFF_TOKEN)).thenReturn(STAFF_USER);
    }

    private AuditLogListItem item() {
        return new AuditLogListItem(
                UUID.fromString(ENTRY), AuditActorType.USER, UUID.fromString(USER), "Maria Ops",
                "EXPERIENCE", UUID.randomUUID(), "experience.updated",
                "{\"note\":\"metadata\"}",
                "[{\"field\":\"name\",\"from\":\"Old\",\"to\":\"New\"}]",
                "req-abc123", Instant.parse("2026-07-26T10:00:00Z"));
    }

    @Test
    void list() throws Exception {
        authenticated();
        when(listAuditLogUseCase.execute(any(), any()))
                .thenReturn(new CursorPage<>(List.of(item()), null));

        mockMvc.perform(get("/api/tour-operators/{id}/audit-log", OP)
                        .header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].context").value("audit-log-entries"))
                // @JsonRawValue: stored JSON emitted as an object, never a quoted string
                .andExpect(jsonPath("$.data[0].changes[0].field").value("name"))
                .andExpect(jsonPath("$.data[0].details.note").value("metadata"))
                .andDo(document("audit-log/list",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(parameterWithName("id").description("The tour operator id")),
                        responseFields(
                                fieldWithPath("data[].id").description("The entry id (UUIDv7 — descending id IS the timeline)"),
                                fieldWithPath("data[].context").description("The entity's collection: \"audit-log-entries\""),
                                fieldWithPath("data[].actorType").description("Who acted: USER or SYSTEM"),
                                fieldWithPath("data[].actorId").description("The acting user's id; null for SYSTEM").optional(),
                                fieldWithPath("data[].actorName").description("The acting user's display name, FROZEN at write time; null for SYSTEM").optional(),
                                fieldWithPath("data[].entityType").description("The audited entity's type, e.g. EXPERIENCE"),
                                fieldWithPath("data[].entityId").description("The audited entity's id"),
                                fieldWithPath("data[].action").description("Dot-namespaced past-tense action, e.g. experience.updated"),
                                subsectionWithPath("data[].details").description("Action metadata that is NOT a field change (per-action shape); null for none").optional(),
                                fieldWithPath("data[].changes[]").description("Field-level change history; null for pure events").optional(),
                                fieldWithPath("data[].changes[].field").description("The changed domain field").optional(),
                                fieldWithPath("data[].changes[].from").description("The value before (null = set from nothing)").optional(),
                                fieldWithPath("data[].changes[].to").description("The value after (null = cleared)").optional(),
                                fieldWithPath("data[].requestId").description("Request correlation id; null off-request").optional(),
                                fieldWithPath("data[].createdAt").description("When the action happened"),
                                fieldWithPath("nextCursor").type(JsonFieldType.STRING).description("Opaque cursor; null on the last page").optional())));
    }

    @Test
    void getOne() throws Exception {
        authenticated();
        when(getAuditLogEntryUseCase.execute(eq(UUID.fromString(OP)), eq(UUID.fromString(ENTRY)), any()))
                .thenReturn(item());

        mockMvc.perform(get("/api/tour-operators/{id}/audit-log/{entryId}", OP, ENTRY)
                        .header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ENTRY))
                .andExpect(jsonPath("$.actorName").value("Maria Ops"))
                .andDo(document("audit-log/get",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(
                                parameterWithName("id").description("The tour operator id"),
                                parameterWithName("entryId").description("The audit entry id")),
                        // The same record the list returns, one level up: every path
                        // here is a `data[].` path there. Keep the two descriptions
                        // in step — a reader lands on whichever they need.
                        responseFields(
                                fieldWithPath("id").description("The entry id (UUIDv7 — descending id IS the timeline)"),
                                fieldWithPath("context").description("The entity's collection: \"audit-log-entries\""),
                                fieldWithPath("actorType").description("Who acted: USER or SYSTEM"),
                                fieldWithPath("actorId").description("The acting user's id; null for SYSTEM").optional(),
                                fieldWithPath("actorName").description("The acting user's display name, FROZEN at write time; null for SYSTEM").optional(),
                                fieldWithPath("entityType").description("The audited entity's type, e.g. EXPERIENCE"),
                                fieldWithPath("entityId").description("The audited entity's id"),
                                fieldWithPath("action").description("Dot-namespaced past-tense action, e.g. experience.updated"),
                                subsectionWithPath("details").description("Action metadata that is NOT a field change (per-action shape); null for none").optional(),
                                fieldWithPath("changes[]").description("Field-level change history; null for pure events").optional(),
                                fieldWithPath("changes[].field").description("The changed domain field").optional(),
                                fieldWithPath("changes[].from").description("The value before (null = set from nothing)").optional(),
                                fieldWithPath("changes[].to").description("The value after (null = cleared)").optional(),
                                fieldWithPath("requestId").description("Request correlation id; null off-request").optional(),
                                fieldWithPath("createdAt").description("When the action happened"))));
    }

    /**
     * <b>The first documented non-2xx response in the guide.</b> Every other
     * operation documents only its happy path, so 60-odd error assertions across
     * the suite are tested and invisible to a reader. This one publishes the
     * error shape rather than leaving a client to discover it in production.
     *
     * <p><b>This is not the non-member 404.</b> A non-member is rejected by
     * {@code TourOperatorMembershipInterceptor} before the controller runs and
     * gets {@code TourOperatorMembershipCheck.TENANT_NOT_FOUND}; this one comes from the use case
     * and says {@code "Audit log entry not found"}. Status and shape match, but
     * {@code message} differs and {@code message} is documented, so a caller can
     * tell them apart.
     *
     * <p>What this snippet <em>does</em> cover is the pair that really is
     * indistinguishable: the lookup is {@code findByIdAndTourOperatorId}, so
     * another operator's entry answers exactly as a missing one does.
     */
    @Test
    void getUnknownIs404() throws Exception {
        authenticated();
        when(getAuditLogEntryUseCase.execute(any(), any(), any()))
                .thenThrow(new ResourceNotFoundException(GetAuditLogEntryUseCase.NOT_FOUND));

        mockMvc.perform(get("/api/tour-operators/{id}/audit-log/{entryId}", OP, MISSING_ENTRY)
                        .header("Authorization", BEARER))
                .andExpect(status().isNotFound())
                // The one assertion holding this sentence. The stub above now derives it
                // from production so the guide follows a reword — but that alone makes the
                // wording unpinned, and this body IS published (PATTERNS §9a, and the
                // loosening #189 shipped). Spell it out here, once.
                .andExpect(jsonPath("$.message").value("Audit log entry not found"))
                .andDo(document("audit-log/get-not-found",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(
                                parameterWithName("id").description("The tour operator id"),
                                parameterWithName("entryId").description("An entry id that does not exist, or belongs to another operator")),
                        responseFields(ApiErrorSnippets.errorFields())));
    }
}

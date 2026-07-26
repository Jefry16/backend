package com.vointika.audit.presentation.controller;

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
                                fieldWithPath("nextCursor").description("Opaque cursor; null on the last page").optional())));
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
                        requestHeaders(headerWithName("Authorization").description("Bearer access token"))));
    }

    @Test
    void getUnknownIs404() throws Exception {
        authenticated();
        when(getAuditLogEntryUseCase.execute(any(), any(), any()))
                .thenThrow(new ResourceNotFoundException("Audit log entry not found"));

        mockMvc.perform(get("/api/tour-operators/{id}/audit-log/{entryId}", OP, ENTRY)
                        .header("Authorization", BEARER))
                .andExpect(status().isNotFound());
    }
}

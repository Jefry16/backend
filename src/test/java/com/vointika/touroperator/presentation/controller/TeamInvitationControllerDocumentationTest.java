package com.vointika.touroperator.presentation.controller;

import com.vointika.shared.web.docs.ApiErrorSnippets;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.port.AccessTokenValidatorPort;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.web.list.ListQueryParser;
import com.vointika.shared.web.security.SecurityConfig;
import com.vointika.touroperator.application.dto.output.InvitationView;
import com.vointika.touroperator.application.usecase.GetInvitationUseCase;
import com.vointika.touroperator.application.usecase.InviteTeamMemberUseCase;
import com.vointika.touroperator.application.usecase.ListInvitationsUseCase;
import com.vointika.touroperator.application.usecase.ResendInvitationUseCase;
import com.vointika.touroperator.application.usecase.RevokeInvitationUseCase;
import com.vointika.touroperator.domain.enums.InvitationStatus;
import com.vointika.touroperator.domain.enums.MemberRole;
import com.vointika.touroperator.infrastructure.web.WebConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.restdocs.payload.JsonFieldType;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

@WebMvcTest(TeamInvitationController.class)
@ExtendWith(RestDocumentationExtension.class)
@Import({SecurityConfig.class, WebConfig.class, ListQueryParser.class})
class TeamInvitationControllerDocumentationTest {

    private static final String OPERATOR_ID = "019f7f33-1833-7dc1-b008-47e6c68b3ea2";
    private static final String MISSING_OP = "019f7f33-0000-7dc1-b008-000000000000";
    private static final String USER_ID = "550e8400-e29b-41d4-a716-446655440000";

    private MockMvc mockMvc;

    @MockitoBean
    private InviteTeamMemberUseCase inviteTeamMemberUseCase;

    @MockitoBean
    private ListInvitationsUseCase listInvitationsUseCase;

    @MockitoBean
    private GetInvitationUseCase getInvitationUseCase;

    @MockitoBean
    private ResendInvitationUseCase resendInvitationUseCase;

    @MockitoBean
    private RevokeInvitationUseCase revokeInvitationUseCase;

    @MockitoBean
    private TourOperatorMembershipCheck membershipCheck;

    @MockitoBean
    private AccessTokenValidatorPort accessTokenValidator;

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
        when(accessTokenValidator.isValid("test-access-token")).thenReturn(true);
        when(accessTokenValidator.extractUserId("test-access-token")).thenReturn(USER_ID);
    }

    @Test
    void invite() throws Exception {
        authenticated();
        when(inviteTeamMemberUseCase.execute(any(), any(), any(), any(), any()))
                .thenReturn(UUID.fromString("aaaaaaaa-0000-4000-8000-000000000001"));

        mockMvc.perform(post("/api/tour-operators/{id}/invitations", OPERATOR_ID)
                        .header("Authorization", "Bearer test-access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "teammate@example.com", "name": "Teammate", "role": "STAFF" }"""))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andDo(document("tour-operators/invitations/create",
                        pathParameters(parameterWithName("id").description("The tour operator id")),
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        requestFields(
                                fieldWithPath("email").description("The invitee's email address"),
                                fieldWithPath("name").description("The invitee's display name (greets them in the invite email)"),
                                fieldWithPath("role").description("The role to grant: ADMIN or STAFF (never OWNER)")),
                        responseHeaders(headerWithName("Location").description("URI of the created invitation"))));
    }

    @Test
    void inviteRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/tour-operators/{id}/invitations", OPERATOR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "teammate@example.com", "name": "Teammate", "role": "STAFF" }"""))
                .andExpect(status().isUnauthorized());
    }

    private static final String INVITATION_ID = "aaaaaaaa-0000-4000-8000-000000000001";
    private static final String INVITER_ID = "bbbbbbbb-0000-4000-8000-000000000002";

    @Test
    void list() throws Exception {
        authenticated();
        when(listInvitationsUseCase.execute(any(), any())).thenReturn(new CursorPage<>(List.of(
                new InvitationView(UUID.fromString(INVITATION_ID), "teammate@example.com", "Teammate",
                        MemberRole.STAFF, InvitationStatus.PENDING, false,
                        Instant.parse("2026-07-21T10:00:00Z"),
                        Instant.parse("2026-07-28T10:00:00Z"), null,
                        UUID.fromString(INVITER_ID), "Olive Owner")),
                "eyJ2MSI6Im5leHQifQ"));

        mockMvc.perform(get("/api/tour-operators/{id}/invitations", OPERATOR_ID)
                        .header("Authorization", "Bearer test-access-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].context").value("invitations"))
                .andExpect(jsonPath("$.data[0].status").value("PENDING"))
                .andExpect(jsonPath("$.data[0].invitedBy.context").value("users"))
                .andExpect(jsonPath("$.data[0].invitedBy.name").value("Olive Owner"))
                .andExpect(jsonPath("$.nextCursor").value("eyJ2MSI6Im5leHQifQ"))
                .andDo(document("tour-operators/invitations/list",
                        pathParameters(parameterWithName("id").description("The tour operator id")),
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        responseFields(
                                fieldWithPath("data[].id").description("The invitation id"),
                                fieldWithPath("data[].context").description("The entity's collection: \"invitations\""),
                                fieldWithPath("data[].email").description("The invitee's email address"),
                                fieldWithPath("data[].name").description("The invitee's display name"),
                                fieldWithPath("data[].role").description("The invited role: ADMIN or STAFF"),
                                fieldWithPath("data[].status").description("Lifecycle state: PENDING, ACCEPTED or REVOKED"),
                                fieldWithPath("data[].expired").description("True when a PENDING invitation is past its expiry window"),
                                fieldWithPath("data[].createdAt").description("When the invitation was issued"),
                                fieldWithPath("data[].expiresAt").description("When the accept link lapses"),
                                fieldWithPath("data[].acceptedAt").type(JsonFieldType.STRING)
                                        .description("When it was accepted, or null").optional(),
                                fieldWithPath("data[].invitedBy.id").description("The inviting user's id"),
                                fieldWithPath("data[].invitedBy.context").description("The entity's collection: \"users\""),
                                fieldWithPath("data[].invitedBy.name").description("The inviter's display name, frozen at issue time (always present)"),
                                fieldWithPath("nextCursor").type(JsonFieldType.STRING).description("Opaque cursor for the next page; null on the last page"))));
    }

    @Test
    void getInvitation() throws Exception {
        authenticated();
        when(getInvitationUseCase.execute(any(), any(), any())).thenReturn(new InvitationView(
                UUID.fromString(INVITATION_ID), "teammate@example.com", "Teammate", MemberRole.STAFF,
                InvitationStatus.PENDING, false,
                java.time.Instant.parse("2026-07-21T10:00:00Z"),
                java.time.Instant.parse("2026-07-28T10:00:00Z"), null,
                UUID.fromString(INVITER_ID), "Olive Owner"));

        mockMvc.perform(get("/api/tour-operators/{id}/invitations/{invitationId}",
                        OPERATOR_ID, INVITATION_ID)
                        .header("Authorization", "Bearer test-access-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invitedBy.context").value("users"))
                .andExpect(jsonPath("$.invitedBy.name").value("Olive Owner"))
                .andDo(document("tour-operators/invitations/get",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(
                                parameterWithName("id").description("The tour operator id"),
                                parameterWithName("invitationId").description("The invitation id")),
                        responseFields(
                                fieldWithPath("id").description("The invitation id"),
                                fieldWithPath("context").description("The entity's collection: \"invitations\""),
                                fieldWithPath("email").description("The invitee's email address"),
                                fieldWithPath("name").description("The invitee's display name"),
                                fieldWithPath("role").description("The invited role: ADMIN or STAFF"),
                                fieldWithPath("status").description("Lifecycle state: PENDING, ACCEPTED or REVOKED"),
                                fieldWithPath("expired").description("True when a PENDING invitation is past its expiry window"),
                                fieldWithPath("createdAt").description("When the invitation was issued"),
                                fieldWithPath("expiresAt").description("When the accept link lapses"),
                                fieldWithPath("acceptedAt").type(JsonFieldType.STRING)
                                        .description("When it was accepted, or null").optional(),
                                fieldWithPath("invitedBy.id").description("The inviting user's id"),
                                fieldWithPath("invitedBy.context").description("The entity's collection: \"users\""),
                                fieldWithPath("invitedBy.name").description("The inviter's display name, frozen at issue time (always present)"))));
    }

    @Test
    void getInvitationRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/tour-operators/{id}/invitations/{invitationId}",
                        OPERATOR_ID, INVITATION_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void resend() throws Exception {
        authenticated();

        mockMvc.perform(post("/api/tour-operators/{id}/invitations/{invitationId}/resend",
                        OPERATOR_ID, INVITATION_ID)
                        .header("Authorization", "Bearer test-access-token"))
                .andExpect(status().isNoContent())
                .andDo(document("tour-operators/invitations/resend",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(
                                parameterWithName("id").description("The tour operator id"),
                                parameterWithName("invitationId").description("The pending invitation to re-send"))));
    }

    @Test
    void revoke() throws Exception {
        authenticated();

        mockMvc.perform(delete("/api/tour-operators/{id}/invitations/{invitationId}",
                        OPERATOR_ID, INVITATION_ID)
                        .header("Authorization", "Bearer test-access-token"))
                .andExpect(status().isNoContent())
                .andDo(document("tour-operators/invitations/revoke",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(
                                parameterWithName("id").description("The tour operator id"),
                                parameterWithName("invitationId").description("The pending invitation to revoke"))));
    }

    @Test
    void resendRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/tour-operators/{id}/invitations/{invitationId}/resend",
                        OPERATOR_ID, INVITATION_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void revokeRequiresAuthentication() throws Exception {
        mockMvc.perform(delete("/api/tour-operators/{id}/invitations/{invitationId}",
                        OPERATOR_ID, INVITATION_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void nonMemberGets404FromTheInterceptor() throws Exception {
        authenticated();
        doThrow(new ResourceNotFoundException("Tour operator not found"))
                .when(membershipCheck).ensureMember(eq(UUID.fromString(USER_ID)), eq(UUID.fromString(MISSING_OP)));

        mockMvc.perform(post("/api/tour-operators/{id}/invitations", MISSING_OP)
                        .header("Authorization", "Bearer test-access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "teammate@example.com", "name": "Teammate", "role": "STAFF" }"""))
                .andExpect(status().isNotFound())
                .andDo(document("tour-operators/invitations/create-not-found",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(parameterWithName("id").description(
                                "An operator you are not a member of. The interceptor answers before the "
                                        + "ADMIN+ gate runs, so a non-member never sees a 403")),
                        responseFields(ApiErrorSnippets.errorFields())));

        Mockito.verifyNoInteractions(inviteTeamMemberUseCase);
    }
}

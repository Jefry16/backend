package com.vointika.touroperator.presentation.controller;

import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.AccessTokenValidatorPort;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.web.security.SecurityConfig;
import com.vointika.touroperator.application.usecase.InviteTeamMemberUseCase;
import com.vointika.touroperator.application.usecase.ResendInvitationUseCase;
import com.vointika.touroperator.application.usecase.RevokeInvitationUseCase;
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
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.http.HttpDocumentation.httpRequest;
import static org.springframework.restdocs.http.HttpDocumentation.httpResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TeamInvitationController.class)
@ExtendWith(RestDocumentationExtension.class)
@Import({SecurityConfig.class, WebConfig.class})
class TeamInvitationControllerDocumentationTest {

    private static final String OPERATOR_ID = "019f7f33-1833-7dc1-b008-47e6c68b3ea2";
    private static final String USER_ID = "550e8400-e29b-41d4-a716-446655440000";

    private MockMvc mockMvc;

    @MockitoBean
    private InviteTeamMemberUseCase inviteTeamMemberUseCase;

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
                        .withResponseDefaults(prettyPrint())
                        .and()
                        .snippets().withDefaults(httpRequest(), httpResponse()))
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
        when(inviteTeamMemberUseCase.execute(any(), any(), any(), any()))
                .thenReturn(UUID.fromString("aaaaaaaa-0000-4000-8000-000000000001"));

        mockMvc.perform(post("/api/tour-operators/{id}/invitations", OPERATOR_ID)
                        .header("Authorization", "Bearer test-access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "teammate@example.com", "role": "STAFF" }"""))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andDo(document("tour-operators/invitations/create",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        requestFields(
                                fieldWithPath("email").description("The invitee's email address"),
                                fieldWithPath("role").description("The role to grant: ADMIN or STAFF (never OWNER)")),
                        responseHeaders(headerWithName("Location").description("URI of the created invitation"))));
    }

    @Test
    void inviteRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/tour-operators/{id}/invitations", OPERATOR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "teammate@example.com", "role": "STAFF" }"""))
                .andExpect(status().isUnauthorized());
    }

    private static final String INVITATION_ID = "aaaaaaaa-0000-4000-8000-000000000001";

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
                .when(membershipCheck).ensureMember(eq(UUID.fromString(USER_ID)), eq(UUID.fromString(OPERATOR_ID)));

        mockMvc.perform(post("/api/tour-operators/{id}/invitations", OPERATOR_ID)
                        .header("Authorization", "Bearer test-access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "teammate@example.com", "role": "STAFF" }"""))
                .andExpect(status().isNotFound());

        Mockito.verifyNoInteractions(inviteTeamMemberUseCase);
    }
}

package com.vointika.touroperator.presentation.controller;

import com.vointika.shared.exception.ConflictException;
import com.vointika.shared.exception.ForbiddenException;
import com.vointika.shared.exception.GoneException;
import com.vointika.shared.port.AccessTokenValidatorPort;
import com.vointika.shared.port.InvitedUserProvisioning.SessionTokens;
import com.vointika.shared.web.security.RefreshTokenCookieFactory;
import com.vointika.shared.web.docs.ApiErrorSnippets;
import com.vointika.shared.web.security.SecurityConfig;
import com.vointika.touroperator.application.usecase.AcceptInvitationUseCase;
import com.vointika.touroperator.application.usecase.GetInvitationPreviewUseCase;
import com.vointika.touroperator.infrastructure.security.TourOperatorPublicRoutes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InvitationAcceptController.class)
@ExtendWith(RestDocumentationExtension.class)
@Import({SecurityConfig.class, TourOperatorPublicRoutes.class})
class InvitationAcceptControllerDocumentationTest {

    private MockMvc mockMvc;

    @MockitoBean
    private AcceptInvitationUseCase acceptInvitationUseCase;

    @MockitoBean
    private GetInvitationPreviewUseCase getInvitationPreviewUseCase;

    @MockitoBean
    private RefreshTokenCookieFactory refreshTokenCookieFactory;

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

    @Test
    void preview() throws Exception {
        when(getInvitationPreviewUseCase.execute("the-token"))
                .thenReturn(new GetInvitationPreviewUseCase.Preview("Acme Tours", "teammate@example.com"));

        mockMvc.perform(get("/api/invitations/{token}/preview", "the-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.context").value("invitation-previews"))
                .andExpect(jsonPath("$.operatorName").value("Acme Tours"))
                .andDo(document("invitations/preview",
                        pathParameters(parameterWithName("token").description("The raw token from the invitation email; it is the capability, so anyone holding it may read this")),
                        responseFields(
                                fieldWithPath("context").description("The entity's collection (always 'invitation-previews')"),
                                fieldWithPath("operatorName").description("The operator that invited you"),
                                fieldWithPath("email").description("The email the invitation was issued to"))));
    }

    @Test
    void acceptAsNewUserAutoLogsIn() throws Exception {
        UUID operatorId = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3ea2");
        when(acceptInvitationUseCase.execute(any(), any(), any(), any()))
                .thenReturn(new AcceptInvitationUseCase.Result(
                        operatorId, "Acme Tours", new SessionTokens("access-jwt", "refresh-tok")));
        when(refreshTokenCookieFactory.issue(any()))
                .thenReturn(ResponseCookie.from("vointika_refresh", "refresh-tok").build());

        mockMvc.perform(post("/api/invitations/{token}/accept", "the-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Ada Lovelace", "password": "Password1!" }"""))
                .andExpect(status().isOk())
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(jsonPath("$.accessToken").value("access-jwt"))
                .andDo(document("invitations/accept",
                        pathParameters(parameterWithName("token").description("The raw token from the invitation email; a fresh resend invalidates the previous one")),
                        requestFields(
                                fieldWithPath("name").description("New user's name (anonymous accept only)"),
                                fieldWithPath("password").description("New user's password (anonymous accept only)")),
                        responseFields(
                                fieldWithPath("id").description("The operator joined"),
                                fieldWithPath("context").description("The entity's collection (always 'tour-operators')"),
                                fieldWithPath("operatorName").description("The operator's display name"),
                                fieldWithPath("accessToken").description("Access token for a newly provisioned user; null when already authenticated"))));
    }

    /**
     * <b>The API's only 410, and the one an invitee is most likely to meet.</b>
     * An accept link lapses after seven days and a resend mints a fresh one, so
     * an old email in the inbox is a dead link rather than a wrong one. 410 says
     * "this existed and is over" — ask for a new invitation; 404 would say
     * "never existed" and invite a retry.
     */
    @Test
    void anExpiredOrRevokedInvitationIsGone() throws Exception {
        when(getInvitationPreviewUseCase.execute("expired-token"))
                .thenThrow(new GoneException("This invitation is no longer valid"));

        mockMvc.perform(get("/api/invitations/{token}/preview", "expired-token"))
                .andExpect(status().isGone())
                .andDo(document("invitations/preview-gone",
                        pathParameters(parameterWithName("token").description(
                                "A token whose invitation was revoked, superseded by a resend, "
                                        + "or left past its seven-day window")),
                        responseFields(ApiErrorSnippets.errorFields())));
    }

    /**
     * <b>The routine failure of this flow.</b> Someone who already has a Vointika
     * account, opened the link while logged out and filled in the form lands here.
     * It is a 409 rather than a silent login because the endpoint will not take a
     * password for an account that already has one. The client's move is to send
     * them to log in and return to the same link — the invitation is still valid.
     */
    @Test
    void acceptingAnonymouslyWithAnExistingAccountIs409() throws Exception {
        when(acceptInvitationUseCase.execute(any(), any(), any(), any()))
                .thenThrow(new ConflictException(
                        "An account with this email already exists — log in to accept the invitation"));

        mockMvc.perform(post("/api/invitations/{token}/accept", "token-for-an-existing-account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Ada Lovelace", "password": "Password1!" }"""))
                .andExpect(status().isConflict())
                .andDo(document("invitations/accept-conflict",
                        pathParameters(parameterWithName("token").description("The raw token from the invitation email")),
                        requestFields(
                                fieldWithPath("name").description(
                                        "Required even here. Both fields are validated before the account "
                                                + "lookup runs, so omitting them is a 422 rather than this "
                                                + "409 — the value is simply unused once the conflict is found"),
                                fieldWithPath("password").description(
                                        "Required even here, for the same reason. Nothing is written to the "
                                                + "existing account")),
                        responseFields(ApiErrorSnippets.errorFields())));
    }

    /**
     * An invitation is issued to one address. A logged-in caller whose account
     * email differs is refused rather than joined, so forwarding the email to a
     * colleague does not hand them the seat. The example sends a Bearer token,
     * because that is what this error turns on.
     */
    @Test
    void acceptingWhileLoggedInAsSomeoneElseIs403() throws Exception {
        when(accessTokenValidator.isValid("test-access-token")).thenReturn(true);
        when(accessTokenValidator.extractUserId("test-access-token"))
                .thenReturn("550e8400-e29b-41d4-a716-446655440000");
        when(acceptInvitationUseCase.execute(any(), any(), any(), any()))
                .thenThrow(new ForbiddenException(
                        "This invitation was issued to a different email address"));

        mockMvc.perform(post("/api/invitations/{token}/accept", "the-token")
                        .header("Authorization", "Bearer test-access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andDo(document("invitations/accept-forbidden",
                        pathParameters(parameterWithName("token").description("The raw token from the invitation email")),
                        requestHeaders(headerWithName("Authorization").description(
                                "Bearer access token — an authenticated accept sends no body")),
                        responseFields(ApiErrorSnippets.errorFields())));
    }
}

package com.vointika.touroperator.presentation.controller;

import com.vointika.shared.port.AccessTokenValidatorPort;
import com.vointika.shared.port.InvitedUserProvisioning.SessionTokens;
import com.vointika.shared.web.security.RefreshTokenCookieFactory;
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
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.http.HttpDocumentation.httpRequest;
import static org.springframework.restdocs.http.HttpDocumentation.httpResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
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
                        .withResponseDefaults(prettyPrint())
                        .and()
                        .snippets().withDefaults(httpRequest(), httpResponse()))
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
                        requestFields(
                                fieldWithPath("name").description("New user's name (anonymous accept only)"),
                                fieldWithPath("password").description("New user's password (anonymous accept only)")),
                        responseFields(
                                fieldWithPath("id").description("The operator joined"),
                                fieldWithPath("context").description("The entity's collection (always 'tour-operators')"),
                                fieldWithPath("operatorName").description("The operator's display name"),
                                fieldWithPath("accessToken").description("Access token for a newly provisioned user; null when already authenticated"))));
    }
}

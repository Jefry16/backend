package com.vointika.touroperator.presentation.controller;

import com.vointika.shared.web.docs.ApiErrorSnippets;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.port.AccessTokenValidatorPort;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.web.security.SecurityConfig;
import com.vointika.touroperator.application.dto.output.StorefrontPasswordView;
import com.vointika.touroperator.application.usecase.GetStorefrontPasswordUseCase;
import com.vointika.touroperator.application.usecase.UpdateStorefrontPasswordUseCase;
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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StorefrontPasswordController.class)
@ExtendWith(RestDocumentationExtension.class)
@Import({SecurityConfig.class, WebConfig.class})
class StorefrontPasswordControllerDocumentationTest {

    private static final String OP = "019f7f33-1833-7dc1-b008-47e6c68b3ea2";
    private static final String USER = "550e8400-e29b-41d4-a716-446655440000";
    private static final String TOKEN = "test-access-token";
    private static final String BEARER = "Bearer " + TOKEN;

    private MockMvc mockMvc;

    @MockitoBean private GetStorefrontPasswordUseCase getUseCase;
    @MockitoBean private UpdateStorefrontPasswordUseCase updateUseCase;
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

    @Test
    void getSettings() throws Exception {
        authenticated();
        when(getUseCase.execute(eq(UUID.fromString(OP)), any()))
                .thenReturn(new StorefrontPasswordView(true, "sunset2026", "We open in August"));

        mockMvc.perform(get("/api/tour-operators/{id}/storefront-password", OP)
                        .header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andDo(document("storefront-password/get",
                        pathParameters(parameterWithName("id").description("The tour operator id")),
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        responseFields(
                                fieldWithPath("enabled").description("Whether the storefront is password-protected"),
                                fieldWithPath("password").type("String").description("The shared gate the operator hands out (member-visible by design; null when never set)").optional(),
                                fieldWithPath("message").type("String").description("The optional \"Message for your visitors\" shown on the theme's password page").optional())));
    }

    @Test
    void update() throws Exception {
        authenticated();

        mockMvc.perform(put("/api/tour-operators/{id}/storefront-password", OP)
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":true,\"password\":\"sunset2026\","
                                + "\"message\":\"We open in August\"}"))
                .andExpect(status().isNoContent())
                .andDo(document("storefront-password/update",
                        pathParameters(parameterWithName("id").description("The tour operator id")),
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        requestFields(
                                fieldWithPath("enabled").description("Turn password protection on/off"),
                                fieldWithPath("password").type("String").description("New password (≤100); null/blank keeps the stored one — enabling with none at all → 422").optional(),
                                fieldWithPath("message").type("String").description("Visitor message (≤1000); null/blank clears it").optional())));
    }

    @Test
    void enablingWithoutPasswordIs422() throws Exception {
        authenticated();
        Mockito.doThrow(new InvalidFieldException("A password is required to enable password protection"))
                .when(updateUseCase).execute(any(), anyBoolean(), any(), any(), any());

        mockMvc.perform(put("/api/tour-operators/{id}/storefront-password", OP)
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":true}"))
                .andExpect(status().isUnprocessableEntity())
                .andDo(document("storefront-password/update-invalid",
                        pathParameters(parameterWithName("id").description("The tour operator id")),
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        requestFields(fieldWithPath("enabled").description(
                                "Turning the gate on with no password stored and none supplied. In practice "
                                        + "an operator created after #157 always has one, so this is reachable "
                                        + "mainly on the older operators that were left open")),
                        responseFields(ApiErrorSnippets.errorFields())));
    }
}

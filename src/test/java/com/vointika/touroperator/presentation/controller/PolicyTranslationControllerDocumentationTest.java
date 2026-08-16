package com.vointika.touroperator.presentation.controller;

import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.AccessTokenValidatorPort;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.web.security.SecurityConfig;
import com.vointika.touroperator.application.dto.output.PolicyTranslationView;
import com.vointika.touroperator.application.usecase.DeletePolicyTranslationUseCase;
import com.vointika.touroperator.application.usecase.ListPolicyTranslationsUseCase;
import com.vointika.touroperator.application.usecase.UpsertPolicyTranslationUseCase;
import com.vointika.touroperator.infrastructure.web.WebConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
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

@WebMvcTest(PolicyTranslationController.class)
@ExtendWith(RestDocumentationExtension.class)
@Import({SecurityConfig.class, WebConfig.class})
class PolicyTranslationControllerDocumentationTest {

    private static final String OPERATOR_ID = "019f7f33-1833-7dc1-b008-47e6c68b3ea2";
    private static final String USER_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final String POLICY_ID = "019f8000-0000-7000-8000-000000000001";
    private static final String BODY =
            "{\"title\":\"Política de cancelación\","
                    + "\"body\":\"<p>Gratis hasta 48h antes.</p>\"}";

    private MockMvc mockMvc;

    @MockitoBean private ListPolicyTranslationsUseCase listUseCase;
    @MockitoBean private UpsertPolicyTranslationUseCase upsertUseCase;
    @MockitoBean private DeletePolicyTranslationUseCase deleteUseCase;
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
        when(accessTokenValidator.isValid("test-access-token")).thenReturn(true);
        when(accessTokenValidator.extractUserId("test-access-token")).thenReturn(USER_ID);
    }

    @Test
    void list() throws Exception {
        authenticated();
        when(listUseCase.execute(any(), any(), any())).thenReturn(List.of(
                new PolicyTranslationView("es", "Política de cancelación", "<p>Gratis</p>")));

        mockMvc.perform(get("/api/tour-operators/{id}/policies/{policyId}/translations",
                        OPERATOR_ID, POLICY_ID)
                        .header("Authorization", "Bearer test-access-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].locale").value("es"))
                .andDo(document("tour-operators/policy-translations/list",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(
                                parameterWithName("id").description("The tour operator id"),
                                parameterWithName("policyId").description("The policy id")),
                        responseFields(
                                fieldWithPath("[].locale").description("The translated locale"),
                                fieldWithPath("[].title")
                                        .description("Translated heading, or null to use the canonical one")
                                        .optional(),
                                fieldWithPath("[].body")
                                        .description("Translated document, or null to use the canonical one")
                                        .optional())));
    }

    @Test
    void upsert() throws Exception {
        authenticated();

        mockMvc.perform(put("/api/tour-operators/{id}/policies/{policyId}/translations/{locale}",
                        OPERATOR_ID, POLICY_ID, "es")
                        .header("Authorization", "Bearer test-access-token")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isNoContent())
                .andDo(document("tour-operators/policy-translations/upsert",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(
                                parameterWithName("id").description("The tour operator id"),
                                parameterWithName("policyId").description(
                                        "The policy id; unknown or another operator's is a 404"),
                                parameterWithName("locale").description(
                                        "Must be one of the operator's supported locales, else 422")),
                        requestFields(
                                fieldWithPath("title")
                                        .description("≤200 chars; blank or absent = untranslated, so the "
                                                + "canonical heading is used").optional(),
                                fieldWithPath("body")
                                        .description("Raw HTML; blank or absent = untranslated, so the "
                                                + "canonical document is used").optional())));
    }

    @Test
    void deleteOne() throws Exception {
        authenticated();

        mockMvc.perform(delete("/api/tour-operators/{id}/policies/{policyId}/translations/{locale}",
                        OPERATOR_ID, POLICY_ID, "es")
                        .header("Authorization", "Bearer test-access-token"))
                .andExpect(status().isNoContent())
                .andDo(document("tour-operators/policy-translations/delete",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(
                                parameterWithName("id").description("The tour operator id"),
                                parameterWithName("policyId").description("The policy id"),
                                parameterWithName("locale").description(
                                        "Deleting an absent overlay is an idempotent success"))));
    }

    @Test
    void translatingAnUnwrittenPolicyIsNotFound() throws Exception {
        authenticated();
        doThrow(new ResourceNotFoundException("Policy not found"))
                .when(upsertUseCase).execute(any(), any(), any(), any(), any());

        mockMvc.perform(put("/api/tour-operators/{id}/policies/{policyId}/translations/{locale}",
                        OPERATOR_ID, POLICY_ID, "es")
                        .header("Authorization", "Bearer test-access-token")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isNotFound());
    }

    @Test
    void anUnsupportedLocaleIsUnprocessable() throws Exception {
        authenticated();
        doThrow(new InvalidFieldException("Locale 'fr' is not supported by this operator"))
                .when(upsertUseCase).execute(any(), any(), any(), any(), any());

        mockMvc.perform(put("/api/tour-operators/{id}/policies/{policyId}/translations/{locale}",
                        OPERATOR_ID, POLICY_ID, "fr")
                        .header("Authorization", "Bearer test-access-token")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnprocessableEntity());
    }
}

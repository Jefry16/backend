package com.vointika.touroperator.presentation.controller;

import com.vointika.shared.web.docs.ApiErrorSnippets;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.AccessTokenValidatorPort;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.web.security.SecurityConfig;
import com.vointika.touroperator.application.dto.output.OperatorLocalesView;
import com.vointika.touroperator.application.usecase.GetOperatorLocalesUseCase;
import com.vointika.touroperator.application.usecase.UpdateOperatorLocalesUseCase;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.patch;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

@WebMvcTest(TourOperatorLocalesController.class)
@ExtendWith(RestDocumentationExtension.class)
@Import({SecurityConfig.class, WebConfig.class})
class TourOperatorLocalesControllerDocumentationTest {

    private static final String OPERATOR_ID = "019f7f33-1833-7dc1-b008-47e6c68b3ea2";
    private static final String MISSING_OP = "019f7f33-0000-7dc1-b008-000000000000";
    private static final String USER_ID = "550e8400-e29b-41d4-a716-446655440000";

    private MockMvc mockMvc;

    @MockitoBean private GetOperatorLocalesUseCase getOperatorLocalesUseCase;
    @MockitoBean private UpdateOperatorLocalesUseCase updateOperatorLocalesUseCase;
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
    void getLocales() throws Exception {
        authenticated();
        when(getOperatorLocalesUseCase.execute(any(), any()))
                .thenReturn(new OperatorLocalesView("es", List.of("en", "es", "fr")));

        mockMvc.perform(get("/api/tour-operators/{id}/locales", OPERATOR_ID)
                        .header("Authorization", "Bearer test-access-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primaryLocale").value("es"))
                .andDo(document("tour-operators/locales/get",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(parameterWithName("id").description("The tour operator id")),
                        responseFields(
                                fieldWithPath("primaryLocale").description("The operator's default content locale"),
                                fieldWithPath("supportedLocales").description("The content locales the operator supports"))));
    }

    @Test
    void updateLocales() throws Exception {
        authenticated();

        mockMvc.perform(patch("/api/tour-operators/{id}/locales", OPERATOR_ID)
                        .header("Authorization", "Bearer test-access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"primaryLocale\": \"es\", \"supportedLocales\": [\"en\", \"es\", \"fr\"] }"))
                .andExpect(status().isNoContent())
                .andDo(document("tour-operators/locales/update",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(parameterWithName("id").description("The tour operator id")),
                        requestFields(
                                fieldWithPath("primaryLocale").description("The default locale; must be one of supportedLocales"),
                                fieldWithPath("supportedLocales").description("Locale codes from GET /api/languages; unknown → 422"))));
    }

    /**
     * Pins the framework behaviour two deleted null-guards relied on.
     *
     * <p>`@RequestBody` is required by default, so an absent body and a literal
     * {@code null} are both rejected with 400 before the handler runs — which is why
     * `body == null ? null : body.x()` in this controller could never fire and was
     * removed. If a Spring upgrade ever changed that, the handler would NPE instead,
     * and only this test would say so.
     */
    @Test
    void nullBodyIsRejectedByTheFrameworkBeforeTheHandlerRuns() throws Exception {
        authenticated();

        mockMvc.perform(patch("/api/tour-operators/{id}/locales", OPERATOR_ID)
                        .header("Authorization", "Bearer test-access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("null"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(patch("/api/tour-operators/{id}/locales", OPERATOR_ID)
                        .header("Authorization", "Bearer test-access-token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateRequiresAuthentication() throws Exception {
        mockMvc.perform(patch("/api/tour-operators/{id}/locales", OPERATOR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"primaryLocale\": \"es\", \"supportedLocales\": [\"es\"] }"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unknownLanguageIsUnprocessable() throws Exception {
        authenticated();
        doThrow(new InvalidFieldException("Unsupported language code: xx"))
                .when(updateOperatorLocalesUseCase).execute(any(), any(), any(), any());

        mockMvc.perform(patch("/api/tour-operators/{id}/locales", OPERATOR_ID)
                        .header("Authorization", "Bearer test-access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"primaryLocale\": \"en\", \"supportedLocales\": [\"en\", \"xx\"] }"))
                .andExpect(status().isUnprocessableEntity())
                .andDo(document("tour-operators/locales/update-invalid",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(parameterWithName("id").description("The tour operator id")),
                        requestFields(
                                fieldWithPath("primaryLocale").description(
                                        "Required on every call — this PATCH replaces both fields, so "
                                                + "sending one alone is a 422"),
                                fieldWithPath("supportedLocales").description(
                                        "Every code must be a platform language (GET /api/languages), and "
                                                + "primaryLocale must be one of them")),
                        responseFields(ApiErrorSnippets.errorFields())));
    }

    @Test
    void nonMemberGets404FromTheInterceptor() throws Exception {
        authenticated();
        doThrow(new ResourceNotFoundException("Tour operator not found"))
                .when(membershipCheck).ensureMember(eq(UUID.fromString(USER_ID)), eq(UUID.fromString(MISSING_OP)));

        mockMvc.perform(get("/api/tour-operators/{id}/locales", MISSING_OP)
                        .header("Authorization", "Bearer test-access-token"))
                .andExpect(status().isNotFound())
                .andDo(document("tour-operators/locales/get-not-found",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(parameterWithName("id").description(
                                "An operator you are not a member of, or one that does not exist. Every "
                                        + "route under /api/tour-operators/{id} answers this way, and the "
                                        + "two cases are deliberately indistinguishable")),
                        responseFields(ApiErrorSnippets.errorFields())));
    }
}

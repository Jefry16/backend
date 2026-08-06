package com.vointika.touroperator.presentation.controller;

import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.port.AccessTokenValidatorPort;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.web.security.SecurityConfig;
import com.vointika.touroperator.application.dto.output.OperatorTranslationView;
import com.vointika.touroperator.application.usecase.DeleteOperatorTranslationUseCase;
import com.vointika.touroperator.application.usecase.GetOperatorTranslationUseCase;
import com.vointika.touroperator.application.usecase.ListOperatorTranslationsUseCase;
import com.vointika.touroperator.application.usecase.UpsertOperatorTranslationUseCase;
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
import static org.springframework.restdocs.http.HttpDocumentation.httpRequest;
import static org.springframework.restdocs.http.HttpDocumentation.httpResponse;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OperatorTranslationController.class)
@ExtendWith(RestDocumentationExtension.class)
@Import({SecurityConfig.class, WebConfig.class})
class OperatorTranslationControllerDocumentationTest {

    private static final String OPERATOR_ID = "019f7f33-1833-7dc1-b008-47e6c68b3ea2";
    private static final String USER_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final String BODY =
            "{\"seoTitle\":\"Acme Tours — buceo\",\"seoDescription\":\"Buceo en grupos pequeños.\","
                    + "\"passwordMessage\":\"Volvemos pronto.\",\"slogan\":\"Bucea con nosotros\","
                    + "\"shortDescription\":\"Salidas diarias en grupos pequeños.\"}";

    private MockMvc mockMvc;

    @MockitoBean private UpsertOperatorTranslationUseCase upsertUseCase;
    @MockitoBean private GetOperatorTranslationUseCase getUseCase;
    @MockitoBean private ListOperatorTranslationsUseCase listUseCase;
    @MockitoBean private DeleteOperatorTranslationUseCase deleteUseCase;
    @MockitoBean private TourOperatorMembershipCheck membershipCheck;
    @MockitoBean private AccessTokenValidatorPort accessTokenValidator;

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

    private static OperatorTranslationView view() {
        return new OperatorTranslationView("es", "Acme Tours — buceo",
                "Buceo en grupos pequeños.", "Volvemos pronto.",
                "Bucea con nosotros", "Salidas diarias en grupos pequeños.");
    }

    @Test
    void list() throws Exception {
        authenticated();
        when(listUseCase.execute(any(), any())).thenReturn(List.of(view()));

        mockMvc.perform(get("/api/tour-operators/{id}/translations", OPERATOR_ID)
                        .header("Authorization", "Bearer test-access-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].locale").value("es"))
                .andDo(document("tour-operators/translations/list",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(parameterWithName("id").description("The tour operator id")),
                        responseFields(
                                fieldWithPath("[].locale").description("The translated locale"),
                                fieldWithPath("[].seoTitle").description("Translated SEO title, or null").optional(),
                                fieldWithPath("[].seoDescription")
                                        .description("Translated meta description, or null").optional(),
                                fieldWithPath("[].passwordMessage")
                                        .description("Translated gate-page copy, or null").optional(),
                                fieldWithPath("[].slogan")
                                        .description("Translated brand slogan, or null").optional(),
                                fieldWithPath("[].shortDescription")
                                        .description("Translated brand short description, or null")
                                        .optional())));
    }

    @Test
    void getOne() throws Exception {
        authenticated();
        when(getUseCase.execute(any(), any(), any())).thenReturn(view());

        mockMvc.perform(get("/api/tour-operators/{id}/translations/{locale}", OPERATOR_ID, "es")
                        .header("Authorization", "Bearer test-access-token"))
                .andExpect(status().isOk())
                .andDo(document("tour-operators/translations/get",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(
                                parameterWithName("id").description("The tour operator id"),
                                parameterWithName("locale").description(
                                        "The locale; an untranslated one returns an empty overlay, not a 404")),
                        responseFields(
                                fieldWithPath("locale").description("The requested locale"),
                                fieldWithPath("seoTitle").description("Translated SEO title, or null").optional(),
                                fieldWithPath("seoDescription")
                                        .description("Translated meta description, or null").optional(),
                                fieldWithPath("passwordMessage")
                                        .description("Translated gate-page copy, or null").optional(),
                                fieldWithPath("slogan")
                                        .description("Translated brand slogan, or null").optional(),
                                fieldWithPath("shortDescription")
                                        .description("Translated brand short description, or null")
                                        .optional())));
    }

    @Test
    void upsert() throws Exception {
        authenticated();

        mockMvc.perform(put("/api/tour-operators/{id}/translations/{locale}", OPERATOR_ID, "es")
                        .header("Authorization", "Bearer test-access-token")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isNoContent())
                .andDo(document("tour-operators/translations/upsert",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(
                                parameterWithName("id").description("The tour operator id"),
                                parameterWithName("locale").description(
                                        "Must be one of the operator's supported locales, else 422")),
                        requestFields(
                                fieldWithPath("seoTitle")
                                        .description("≤70 chars; blank or absent = untranslated, so the "
                                                + "canonical value is used").optional(),
                                fieldWithPath("seoDescription")
                                        .description("≤320 chars; blank or absent = untranslated").optional(),
                                fieldWithPath("passwordMessage")
                                        .description("Gate-page copy for this locale; blank or absent = "
                                                + "untranslated").optional(),
                                fieldWithPath("slogan")
                                        .description("≤80 chars; blank or absent = untranslated").optional(),
                                fieldWithPath("shortDescription")
                                        .description("≤150 chars; blank or absent = untranslated")
                                        .optional())));
    }

    @Test
    void deleteOne() throws Exception {
        authenticated();

        mockMvc.perform(delete("/api/tour-operators/{id}/translations/{locale}", OPERATOR_ID, "es")
                        .header("Authorization", "Bearer test-access-token"))
                .andExpect(status().isNoContent())
                .andDo(document("tour-operators/translations/delete",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(
                                parameterWithName("id").description("The tour operator id"),
                                parameterWithName("locale").description(
                                        "Deleting an absent overlay is an idempotent success"))));
    }

    @Test
    void anUnsupportedLocaleIsUnprocessable() throws Exception {
        authenticated();
        doThrow(new InvalidFieldException("Locale 'fr' is not supported by this operator"))
                .when(upsertUseCase).execute(any(), any(), any(), any());

        mockMvc.perform(put("/api/tour-operators/{id}/translations/{locale}", OPERATOR_ID, "fr")
                        .header("Authorization", "Bearer test-access-token")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/tour-operators/{id}/translations", OPERATOR_ID))
                .andExpect(status().isUnauthorized());
    }
}

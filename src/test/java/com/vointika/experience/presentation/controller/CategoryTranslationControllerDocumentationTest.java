package com.vointika.experience.presentation.controller;

import com.vointika.experience.application.usecase.DeleteCategoryTranslationUseCase;
import com.vointika.experience.application.usecase.GetCategoryTranslationUseCase;
import com.vointika.experience.application.usecase.ListCategoryTranslationsUseCase;
import com.vointika.experience.application.usecase.UpsertCategoryTranslationUseCase;
import com.vointika.experience.domain.entity.CategoryTranslation;
import com.vointika.experience.domain.valueobject.CategoryName;
import com.vointika.shared.exception.ForbiddenException;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.port.AccessTokenValidatorPort;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.service.OperatorLocaleCheck;
import com.vointika.shared.valueobject.LocaleCode;
import com.vointika.shared.web.docs.ApiErrorSnippets;
import com.vointika.shared.web.security.SecurityConfig;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
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

@WebMvcTest(CategoryTranslationController.class)
@ExtendWith(RestDocumentationExtension.class)
@Import({SecurityConfig.class, WebConfig.class})
class CategoryTranslationControllerDocumentationTest {

    private static final String OP = "019f7f33-1833-7dc1-b008-47e6c68b3ea2";
    private static final String CAT = "cccccccc-0000-4000-8000-000000000001";
    private static final String USER = "550e8400-e29b-41d4-a716-446655440000";
    private static final String TOKEN = "test-access-token";
    private static final String BEARER = "Bearer " + TOKEN;

    private static final String STAFF_TOKEN = "staff-access-token";
    private static final String STAFF_BEARER = "Bearer " + STAFF_TOKEN;
    private static final String STAFF_USER = "550e8400-e29b-41d4-a716-4466554400ff";

    private static final String BODY = "{\"name\":\"Excursiones en barco\"}";
    private static final String PATH =
            "/api/tour-operators/{id}/categories/{categoryId}/translations";
    private static final String LOCALE_PATH = PATH + "/{locale}";

    private MockMvc mockMvc;

    @MockitoBean private UpsertCategoryTranslationUseCase upsertUseCase;
    @MockitoBean private GetCategoryTranslationUseCase getUseCase;
    @MockitoBean private ListCategoryTranslationsUseCase listUseCase;
    @MockitoBean private DeleteCategoryTranslationUseCase deleteUseCase;
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

    private CategoryTranslation translation() {
        return new CategoryTranslation(UUID.fromString(CAT), UUID.fromString(OP),
                new LocaleCode("es"), new CategoryName("Excursiones en barco"));
    }

    @Test
    void list() throws Exception {
        authenticated();
        when(listUseCase.execute(any(), any(), any())).thenReturn(List.of(translation()));

        mockMvc.perform(get(PATH, OP, CAT).header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].locale").value("es"))
                .andExpect(jsonPath("$[0].name").value("Excursiones en barco"))
                .andDo(document("category-translations/list",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(
                                parameterWithName("id").description("The tour operator id"),
                                parameterWithName("categoryId").description("The category id")),
                        responseFields(
                                fieldWithPath("[].locale").description("The content locale this row translates into"),
                                fieldWithPath("[].name").description(
                                        "The translated category name; null where the canonical value still serves").optional())));
    }

    @Test
    void getOne() throws Exception {
        authenticated();
        when(getUseCase.execute(any(), any(), any(), any())).thenReturn(translation());

        mockMvc.perform(get(LOCALE_PATH, OP, CAT, "es").header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locale").value("es"))
                .andDo(document("category-translations/get",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(
                                parameterWithName("id").description("The tour operator id"),
                                parameterWithName("categoryId").description("The category id"),
                                parameterWithName("locale").description("BCP-47 locale code")),
                        responseFields(
                                fieldWithPath("locale").description("The content locale this row translates into"),
                                fieldWithPath("name").description(
                                        "The translated category name; null where the canonical value still serves").optional())));
    }

    @Test
    void upsert() throws Exception {
        authenticated();

        mockMvc.perform(put(LOCALE_PATH, OP, CAT, "es")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isNoContent())
                .andDo(document("category-translations/upsert",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(
                                parameterWithName("id").description("The tour operator id"),
                                parameterWithName("categoryId").description("The category id"),
                                parameterWithName("locale").description(
                                        "A content locale the operator supports — one it does not is a 422")),
                        // A blank name deletes the overlay rather than storing one —
                        // an empty row and no row mean the same thing to a reader.
                        requestFields(
                                fieldWithPath("name").description(
                                        "The translated category name; blank or null clears the overlay for this locale").optional())));
    }

    @Test
    void deleteOne() throws Exception {
        authenticated();

        mockMvc.perform(delete(LOCALE_PATH, OP, CAT, "es").header("Authorization", BEARER))
                .andExpect(status().isNoContent())
                .andDo(document("category-translations/delete",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(
                                parameterWithName("id").description("The tour operator id"),
                                parameterWithName("categoryId").description("The category id"),
                                parameterWithName("locale").description("BCP-47 locale code"))));
    }

    @Test
    void upsertRequiresAuthentication() throws Exception {
        mockMvc.perform(put(LOCALE_PATH, OP, CAT, "es")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void staffCannotUpsert() throws Exception {
        authenticatedAsStaff();
        doThrow(new ForbiddenException(TourOperatorMembershipCheck.requiresRoleMessage("ADMIN")))
                .when(upsertUseCase).execute(any(), any(), any(), any(), any());

        mockMvc.perform(put(LOCALE_PATH, OP, CAT, "es")
                        .header("Authorization", STAFF_BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden())
                .andDo(document("category-translations/upsert-forbidden",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(
                                parameterWithName("id").description("The tour operator id"),
                                parameterWithName("categoryId").description("The category id"),
                                parameterWithName("locale").description(
                                        "A content locale the operator supports — one it does not is a 422")),
                        responseFields(ApiErrorSnippets.errorFields())));
    }

    @Test
    void aMissingBodyIs400BeforeTheHandlerRuns() throws Exception {
        authenticated();

        mockMvc.perform(put(LOCALE_PATH, OP, CAT, "es")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andDo(document("category-translations/upsert-missing-body",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(
                                parameterWithName("id").description("The tour operator id"),
                                parameterWithName("categoryId").description("The category id"),
                                parameterWithName("locale").description(
                                        "A content locale the operator supports — one it does not is a 422")),
                        responseFields(ApiErrorSnippets.errorFields())));

        verifyNoInteractions(upsertUseCase);
    }

    @Test
    void unsupportedLocaleIs422() throws Exception {
        authenticated();
        doThrow(new InvalidFieldException(OperatorLocaleCheck.refusal("fr")))
                .when(upsertUseCase).execute(any(), any(), any(), any(), any());

        mockMvc.perform(put(LOCALE_PATH, OP, CAT, "fr")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnprocessableEntity())
                .andDo(document("category-translations/upsert-unsupported-locale",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(
                                parameterWithName("id").description("The tour operator id"),
                                parameterWithName("categoryId").description("The category id"),
                                parameterWithName("locale").description(
                                        "A content locale the operator supports — one it does not is a 422")),
                        responseFields(ApiErrorSnippets.errorFields())));
    }
}

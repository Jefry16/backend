package com.vointika.experience.presentation.controller;

import com.vointika.shared.service.OperatorLocaleCheck;
import com.vointika.experience.application.dto.output.ExperienceTranslationView;
import com.vointika.experience.application.usecase.DeleteExperienceTranslationUseCase;
import com.vointika.experience.application.usecase.GetExperienceTranslationUseCase;
import com.vointika.experience.application.usecase.ListExperienceTranslationsUseCase;
import com.vointika.experience.application.usecase.UpsertExperienceTranslationUseCase;
import com.vointika.shared.exception.ForbiddenException;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.AccessTokenValidatorPort;
import com.vointika.shared.port.TourOperatorMembershipCheck;
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
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExperienceTranslationController.class)
@ExtendWith(RestDocumentationExtension.class)
@Import({SecurityConfig.class, WebConfig.class})
class ExperienceTranslationControllerDocumentationTest {

    private static final String OP = "019f7f33-1833-7dc1-b008-47e6c68b3ea2";
    private static final String EXP = "aaaaaaaa-0000-4000-8000-000000000001";
    private static final String USER = "550e8400-e29b-41d4-a716-446655440000";

    private MockMvc mockMvc;

    @MockitoBean private UpsertExperienceTranslationUseCase upsertUseCase;
    @MockitoBean private GetExperienceTranslationUseCase getUseCase;
    @MockitoBean private ListExperienceTranslationsUseCase listUseCase;
    @MockitoBean private DeleteExperienceTranslationUseCase deleteUseCase;
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
        when(accessTokenValidator.extractUserId("test-access-token")).thenReturn(USER);
    }

    private ExperienceTranslationView esView() {
        return new ExperienceTranslationView("es", "Buceo al atardecer", "Buceo en el arrecife",
                "Descripción larga…", "buceo-al-atardecer",
                "Buceo al atardecer | Acme Tours", "Buceo guiado en el arrecife al anochecer.");
    }

    private static final String BODY = """
            {
              "name": "Buceo al atardecer",
              "description": "Buceo en el arrecife",
              "longDescription": "Descripción larga…",
              "handle": "buceo-al-atardecer"
            }""";

    @Test
    void list() throws Exception {
        authenticated();
        when(listUseCase.execute(any(), any(), any())).thenReturn(List.of(esView()));

        mockMvc.perform(get("/api/tour-operators/{id}/experiences/{experienceId}/translations", OP, EXP)
                        .header("Authorization", "Bearer test-access-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].locale").value("es"))
                .andDo(document("experiences/translations/list",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(
                                parameterWithName("id").description("The tour operator id"),
                                parameterWithName("experienceId").description("The experience id")),
                        responseFields(
                                fieldWithPath("[].locale").description("The content locale this row translates into"),
                                fieldWithPath("[].name").description("Translated name; null where the canonical value serves").optional(),
                                fieldWithPath("[].description").description("Translated short description; null where the canonical value serves").optional(),
                                fieldWithPath("[].longDescription").description("Translated long description; null where the canonical value serves").optional(),
                                fieldWithPath("[].handle").description("Localized handle; null where the canonical handle serves this locale").optional(),
                                fieldWithPath("[].seoTitle").type(JsonFieldType.STRING).description("Translated SEO title").optional(),
                                fieldWithPath("[].seoDescription").type(JsonFieldType.STRING).description("Translated SEO description").optional())));
    }

    @Test
    void getTranslation() throws Exception {
        authenticated();
        when(getUseCase.execute(any(), any(), any(), any())).thenReturn(esView());

        mockMvc.perform(get("/api/tour-operators/{id}/experiences/{experienceId}/translations/{locale}", OP, EXP, "es")
                        .header("Authorization", "Bearer test-access-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locale").value("es"))
                // The upsert has always accepted these two — the input record it takes
                // as its body carries them and the use case writes them — but nothing
                // could read them back, so a whole-replace save cleared them.
                .andExpect(jsonPath("$.seoTitle").value("Buceo al atardecer | Acme Tours"))
                .andExpect(jsonPath("$.seoDescription")
                        .value("Buceo guiado en el arrecife al anochecer."))
                .andDo(document("experiences/translations/get",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(
                                parameterWithName("id").description("The tour operator id"),
                                parameterWithName("experienceId").description("The experience id"),
                                parameterWithName("locale").description("BCP-47 locale code")),
                        responseFields(
                                fieldWithPath("locale").description("The content locale this row translates into"),
                                fieldWithPath("name").description("Translated name; null where the canonical value serves").optional(),
                                fieldWithPath("description").description("Translated short description; null where the canonical value serves").optional(),
                                fieldWithPath("longDescription").description("Translated long description; null where the canonical value serves").optional(),
                                fieldWithPath("handle").description("Localized handle; null where the canonical handle serves this locale").optional(),
                                fieldWithPath("seoTitle").type(JsonFieldType.STRING).description("Translated SEO title").optional(),
                                fieldWithPath("seoDescription").type(JsonFieldType.STRING).description("Translated SEO description").optional())));
    }

    @Test
    void upsert() throws Exception {
        authenticated();
        mockMvc.perform(put("/api/tour-operators/{id}/experiences/{experienceId}/translations/{locale}", OP, EXP, "es")
                        .header("Authorization", "Bearer test-access-token")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isNoContent())
                .andDo(document("experiences/translations/upsert",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(
                                parameterWithName("id").description("The tour operator id"),
                                parameterWithName("experienceId").description("The experience id"),
                                parameterWithName("locale").description("A locale the operator supports (else 422)")),
                        requestFields(
                                fieldWithPath("name").description("Translated name (null = fall back to canonical)").optional(),
                                fieldWithPath("description").description("Translated short description").optional(),
                                fieldWithPath("longDescription").description("Translated long description").optional(),
                                fieldWithPath("handle").description("Optional localized handle; must not be taken by another experience — as a localized handle in this locale, or as its canonical handle (409 on dup). If omitted, derived from name").optional())));
    }

    @Test
    void deleteTranslation() throws Exception {
        authenticated();
        mockMvc.perform(delete("/api/tour-operators/{id}/experiences/{experienceId}/translations/{locale}", OP, EXP, "es")
                        .header("Authorization", "Bearer test-access-token"))
                .andExpect(status().isNoContent())
                .andDo(document("experiences/translations/delete",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(
                                parameterWithName("id").description("The tour operator id"),
                                parameterWithName("experienceId").description("The experience id"),
                                parameterWithName("locale").description("BCP-47 locale code"))));
    }

    @Test
    void upsertRequiresAuthentication() throws Exception {
        mockMvc.perform(put("/api/tour-operators/{id}/experiences/{experienceId}/translations/{locale}", OP, EXP, "es")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void staffCannotUpsert() throws Exception {
        authenticated();
        doThrow(new ForbiddenException("This action requires ADMIN privileges"))
                .when(upsertUseCase).execute(any(), any(), any(), any(), any());
        mockMvc.perform(put("/api/tour-operators/{id}/experiences/{experienceId}/translations/{locale}", OP, EXP, "es")
                        .header("Authorization", "Bearer test-access-token")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void unsupportedLocaleIsUnprocessable() throws Exception {
        authenticated();
        doThrow(new InvalidFieldException(OperatorLocaleCheck.refusal("de")))
                .when(upsertUseCase).execute(any(), any(), any(), any(), any());
        mockMvc.perform(put("/api/tour-operators/{id}/experiences/{experienceId}/translations/{locale}", OP, EXP, "de")
                        .header("Authorization", "Bearer test-access-token")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void nonMemberGets404FromTheInterceptor() throws Exception {
        authenticated();
        doThrow(new ResourceNotFoundException(TourOperatorMembershipCheck.TENANT_NOT_FOUND))
                .when(membershipCheck).ensureMember(eq(UUID.fromString(USER)), eq(UUID.fromString(OP)));
        mockMvc.perform(get("/api/tour-operators/{id}/experiences/{experienceId}/translations", OP, EXP)
                        .header("Authorization", "Bearer test-access-token"))
                .andExpect(status().isNotFound());
    }
}

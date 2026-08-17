package com.vointika.metafield.presentation.controller;

import com.vointika.metafield.domain.valueobject.MetafieldType;
import com.vointika.metafield.application.usecase.DeleteMetafieldTranslationsUseCase;
import com.vointika.metafield.application.usecase.GetMetafieldTranslationsUseCase;
import com.vointika.metafield.application.usecase.ListMetafieldTranslationLocalesUseCase;
import com.vointika.metafield.application.usecase.UpsertMetafieldTranslationsUseCase;
import com.vointika.metafield.domain.valueobject.MetafieldOwnerType;
import com.vointika.shared.port.AccessTokenValidatorPort;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.web.list.ListQueryParser;
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
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Per-locale overlays for one experience's metafields. Same flow as its two twins.
 */
@WebMvcTest(ExperienceMetafieldTranslationController.class)
@ExtendWith(RestDocumentationExtension.class)
@Import({SecurityConfig.class, WebConfig.class, ListQueryParser.class})
class ExperienceMetafieldTranslationControllerDocumentationTest {

    private static final String OP = "019f7f33-1833-7dc1-b008-47e6c68b3ea2";
    private static final String EXPERIENCE_ID = "019f7f33-1833-7dc1-b008-47e6c68b3eb4";
    private static final String USER = "550e8400-e29b-41d4-a716-446655440000";
    private static final String TOKEN = "test-access-token";
    private static final String BEARER = "Bearer " + TOKEN;

    private MockMvc mockMvc;

    @MockitoBean private ListMetafieldTranslationLocalesUseCase listLocalesUseCase;
    @MockitoBean private GetMetafieldTranslationsUseCase getUseCase;
    @MockitoBean private UpsertMetafieldTranslationsUseCase upsertUseCase;
    @MockitoBean private DeleteMetafieldTranslationsUseCase deleteUseCase;
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
        when(accessTokenValidator.isValid(TOKEN)).thenReturn(true);
        when(accessTokenValidator.extractUserId(TOKEN)).thenReturn(USER);
    }

    @Test
    void listLocales() throws Exception {
        when(listLocalesUseCase.execute(any(), any(), eq(MetafieldOwnerType.EXPERIENCE), any()))
                .thenReturn(List.of("en", "fr"));

        mockMvc.perform(get("/api/tour-operators/{id}/experiences/{experienceId}/metafield-translations", OP, EXPERIENCE_ID)
                        .header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("en"))
                .andDo(document("experience-metafield-translations/list-locales",
                        pathParameters(parameterWithName("id").description("The tour operator id"),
                                parameterWithName("experienceId").description(
                                        "The experience whose values these overlay. One that does not "
                                                + "exist, or belongs to another operator, is a 404")),
                        responseFields(fieldWithPath("[]").description(
                                "Locale codes that have at least one translated metafield — the editor's "
                                        + "\"started\" markers. A locale absent here is untranslated, not "
                                        + "unsupported")),
                        requestHeaders(headerWithName("Authorization")
                                .description("Bearer access token"))));
    }

    @Test
    void getOne() throws Exception {
        when(getUseCase.execute(any(), any(), eq(MetafieldOwnerType.EXPERIENCE), any(), anyString()))
                .thenReturn(Map.of("custom.opening-hours", "Mon-Fri 09:00-18:00"));

        mockMvc.perform(get("/api/tour-operators/{id}/experiences/{experienceId}/metafield-translations/{locale}", OP, EXPERIENCE_ID, "en")
                        .header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['custom.opening-hours']").value("Mon-Fri 09:00-18:00"))
                .andDo(document("experience-metafield-translations/get",
                        pathParameters(parameterWithName("id").description("The tour operator id"),
                                parameterWithName("experienceId").description(
                                        "The experience whose values these overlay. One that does not "
                                                + "exist, or belongs to another operator, is a 404"),
                                parameterWithName("locale").description(
                                        "The locale to read. An untranslated one returns {} rather than a 404")),
                        responseFields(fieldWithPath("*").description(
                                "One entry per translated metafield, keyed \"namespace.key\". Only keys "
                                        + "with an overlay in this locale appear; everything else falls back "
                                        + "to the canonical value")),
                        requestHeaders(headerWithName("Authorization")
                                .description("Bearer access token"))));
    }

    @Test
    void upsert() throws Exception {
        mockMvc.perform(put("/api/tour-operators/{id}/experiences/{experienceId}/metafield-translations/{locale}", OP, EXPERIENCE_ID, "en")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"values\":{\"custom.opening-hours\":\"Mon-Fri 09:00-18:00\"}}"))
                .andExpect(status().isNoContent())
                .andDo(document("experience-metafield-translations/upsert",
                        pathParameters(parameterWithName("id").description("The tour operator id"),
                                parameterWithName("experienceId").description(
                                        "The experience whose values these overlay. One that does not "
                                                + "exist, or belongs to another operator, is a 404"),
                                parameterWithName("locale").description(
                                        "The locale being written. It must be one the operator "
                                                + "publishes, not merely a well-formed code — an "
                                                + "unpublished one is a 422. Only the upsert checks "
                                                + "this; the read and the delete do not")),
                        requestFields(subsectionWithPath("values").description(
                                "The locale's overlays, keyed \"namespace.key\" — a bare key is a 422. A "
                                        + "blank value CLEARS that key; a key left out is UNTOUCHED, so a "
                                        + "partial form cannot delete what it never rendered. Only "
                                        + MetafieldType.translatableCodes() + " metafields may appear, and "
                                        + "each must already have a value to overlay. Nothing is written if "
                                        + "any entry is rejected")),
                        requestHeaders(headerWithName("Authorization")
                                .description("Bearer access token"))));
    }

    @Test
    void deleteLocale() throws Exception {
        mockMvc.perform(delete("/api/tour-operators/{id}/experiences/{experienceId}/metafield-translations/{locale}", OP, EXPERIENCE_ID, "en")
                        .header("Authorization", BEARER))
                .andExpect(status().isNoContent())
                .andDo(document("experience-metafield-translations/delete",
                        pathParameters(parameterWithName("id").description("The tour operator id"),
                                parameterWithName("experienceId").description(
                                        "The experience whose values these overlay. One that does not "
                                                + "exist, or belongs to another operator, is a 404"),
                                parameterWithName("locale").description(
                                        "The locale to drop whole. 204 whether or not anything was there")),
                        requestHeaders(headerWithName("Authorization")
                                .description("Bearer access token"))));
    }
}

package com.vointika.metafield.presentation.controller;

import com.vointika.metafield.domain.valueobject.MetafieldType;
import com.vointika.metafield.application.usecase.DeleteMetaobjectFieldTranslationsUseCase;
import com.vointika.metafield.application.usecase.GetMetaobjectFieldTranslationsUseCase;
import com.vointika.metafield.application.usecase.ListMetaobjectFieldTranslationLocalesUseCase;
import com.vointika.metafield.application.usecase.UpsertMetaobjectFieldTranslationsUseCase;
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
 * Per-locale overlays for a metaobject entry's fields. Keys are bare field keys
 * — a metaobject field has no namespace, which is the one way this differs from
 * its three metafield twins.
 */
@WebMvcTest(MetaobjectFieldTranslationController.class)
@ExtendWith(RestDocumentationExtension.class)
@Import({SecurityConfig.class, WebConfig.class, ListQueryParser.class})
class MetaobjectFieldTranslationControllerDocumentationTest {

    private static final String OP = "019f7f33-1833-7dc1-b008-47e6c68b3ea2";
    private static final String ENTRY = "01900000-0000-7000-8000-0000000000d5";
    private static final String USER = "550e8400-e29b-41d4-a716-446655440000";
    private static final String TOKEN = "test-access-token";
    private static final String BEARER = "Bearer " + TOKEN;

    private MockMvc mockMvc;

    @MockitoBean private ListMetaobjectFieldTranslationLocalesUseCase listLocalesUseCase;
    @MockitoBean private GetMetaobjectFieldTranslationsUseCase getUseCase;
    @MockitoBean private UpsertMetaobjectFieldTranslationsUseCase upsertUseCase;
    @MockitoBean private DeleteMetaobjectFieldTranslationsUseCase deleteUseCase;
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
        when(listLocalesUseCase.execute(any(), any(), any())).thenReturn(List.of("en"));

        mockMvc.perform(get("/api/tour-operators/{id}/metaobjects/{metaobjectId}/field-translations",
                        OP, ENTRY)
                        .header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("en"))
                .andDo(document("metaobject-field-translations/list-locales",
                        pathParameters(parameterWithName("id").description("The tour operator id"),
                                parameterWithName("metaobjectId").description(
                                        "The metaobject entry whose fields these overlay. One that does "
                                                + "not exist, or belongs to another operator, is a 404")),
                        responseFields(fieldWithPath("[]").description(
                                "Locale codes that have at least one translated metafield — the editor's "
                                        + "\"started\" markers. A locale absent here is untranslated, not "
                                        + "unsupported")),
                        requestHeaders(headerWithName("Authorization")
                                .description("Bearer access token"))));
    }

    @Test
    void getOne() throws Exception {
        when(getUseCase.execute(any(), any(), any(), anyString()))
                .thenReturn(Map.of("notes", "Sloop, 11 m. Refitted in 2022."));

        mockMvc.perform(get("/api/tour-operators/{id}/metaobjects/{metaobjectId}/field-translations/{locale}",
                        OP, ENTRY, "en")
                        .header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notes").value("Sloop, 11 m. Refitted in 2022."))
                .andDo(document("metaobject-field-translations/get",
                        pathParameters(parameterWithName("id").description("The tour operator id"),
                                parameterWithName("metaobjectId").description(
                                        "The metaobject entry whose fields these overlay. One that does "
                                                + "not exist, or belongs to another operator, is a 404"),
                                parameterWithName("locale").description(
                                        "The locale to read. An untranslated one returns {} rather than a 404")),
                        responseFields(fieldWithPath("*").description(
                                "One entry per translated field, keyed by the BARE field key — a "
                                        + "metaobject field has no namespace, unlike a metafield. Only keys "
                                        + "with an overlay in this locale appear; everything else falls back "
                                        + "to the canonical value")),
                        requestHeaders(headerWithName("Authorization")
                                .description("Bearer access token"))));
    }

    @Test
    void upsert() throws Exception {
        mockMvc.perform(put("/api/tour-operators/{id}/metaobjects/{metaobjectId}/field-translations/{locale}",
                        OP, ENTRY, "en")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"values\":{\"notes\":\"Sloop, 11 m. Refitted in 2022.\"}}"))
                .andExpect(status().isNoContent())
                .andDo(document("metaobject-field-translations/upsert",
                        pathParameters(parameterWithName("id").description("The tour operator id"),
                                parameterWithName("metaobjectId").description(
                                        "The metaobject entry whose fields these overlay. One that does "
                                                + "not exist, or belongs to another operator, is a 404"),
                                parameterWithName("locale").description(
                                        "The locale being written. It must be one the operator "
                                                + "publishes, not merely a well-formed code — an "
                                                + "unpublished one is a 422. Only the upsert checks "
                                                + "this; the read and the delete do not")),
                        requestFields(subsectionWithPath("values").description(
                                "The locale's overlays, keyed by the BARE field key — a metaobject field "
                                        + "has no namespace, so a dotted \"namespace.key\" is a 404. A "
                                        + "blank value CLEARS that key; a key left out is UNTOUCHED, so a "
                                        + "partial form cannot delete what it never rendered. Only "
                                        + MetafieldType.translatableCodes() + " fields may appear, and each "
                                        + "must already have a value to overlay. Nothing is written if any "
                                        + "entry is rejected")),
                        requestHeaders(headerWithName("Authorization")
                                .description("Bearer access token"))));
    }

    @Test
    void deleteLocale() throws Exception {
        mockMvc.perform(delete("/api/tour-operators/{id}/metaobjects/{metaobjectId}/field-translations/{locale}",
                        OP, ENTRY, "en")
                        .header("Authorization", BEARER))
                .andExpect(status().isNoContent())
                .andDo(document("metaobject-field-translations/delete",
                        pathParameters(parameterWithName("id").description("The tour operator id"),
                                parameterWithName("metaobjectId").description(
                                        "The metaobject entry whose fields these overlay. One that does "
                                                + "not exist, or belongs to another operator, is a 404"),
                                parameterWithName("locale").description(
                                        "The locale to drop whole. 204 whether or not anything was there")),
                        requestHeaders(headerWithName("Authorization")
                                .description("Bearer access token"))));
    }
}

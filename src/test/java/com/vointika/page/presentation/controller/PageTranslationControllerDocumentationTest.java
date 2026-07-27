package com.vointika.page.presentation.controller;

import com.vointika.page.application.usecase.DeletePageTranslationUseCase;
import com.vointika.page.application.usecase.GetPageTranslationUseCase;
import com.vointika.page.application.usecase.ListPageTranslationsUseCase;
import com.vointika.page.application.usecase.UpsertPageTranslationUseCase;
import com.vointika.page.domain.entity.PageTranslation;
import com.vointika.page.domain.valueobject.PageBody;
import com.vointika.page.domain.valueobject.PageTitle;
import com.vointika.shared.port.AccessTokenValidatorPort;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.valueobject.LocaleCode;
import com.vointika.shared.valueobject.Slug;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PageTranslationController.class)
@ExtendWith(RestDocumentationExtension.class)
@Import({SecurityConfig.class, WebConfig.class, ListQueryParser.class})
class PageTranslationControllerDocumentationTest {

    private static final String OP = "019f7f33-1833-7dc1-b008-47e6c68b3ea2";
    private static final String PAGE = "bbbbbbbb-0000-4000-8000-000000000001";
    private static final String USER = "550e8400-e29b-41d4-a716-446655440000";
    private static final String TOKEN = "test-access-token";
    private static final String BEARER = "Bearer " + TOKEN;

    private MockMvc mockMvc;

    @MockitoBean private ListPageTranslationsUseCase listUseCase;
    @MockitoBean private GetPageTranslationUseCase getUseCase;
    @MockitoBean private UpsertPageTranslationUseCase upsertUseCase;
    @MockitoBean private DeletePageTranslationUseCase deleteUseCase;
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

    private PageTranslation translation() {
        return new PageTranslation(UUID.fromString(PAGE), UUID.fromString(OP),
                new LocaleCode("es"), new PageTitle("Sobre nosotros"),
                new PageBody("<p>Hola</p>"), null, null, new Slug("sobre-nosotros"));
    }

    @Test
    void list() throws Exception {
        authenticated();
        when(listUseCase.execute(eq(UUID.fromString(OP)), eq(UUID.fromString(PAGE)), any()))
                .thenReturn(List.of(translation()));

        mockMvc.perform(get("/api/tour-operators/{id}/pages/{pageId}/translations", OP, PAGE)
                        .header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].locale").value("es"))
                .andDo(document("page-translations/list",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        responseFields(
                                fieldWithPath("[].locale").description("The overlay's locale"),
                                fieldWithPath("[].title").description("Translated title; null = falls back to canonical").optional(),
                                fieldWithPath("[].body").description("Translated raw-HTML body; null = falls back").optional(),
                                fieldWithPath("[].seoTitle").type("String").description("Translated SEO title; null = falls back").optional(),
                                fieldWithPath("[].seoDescription").type("String").description("Translated SEO description; null = falls back").optional(),
                                fieldWithPath("[].slug").description("Localized handle; null = the canonical handle serves this locale").optional())));
    }

    @Test
    void getOne() throws Exception {
        authenticated();
        when(getUseCase.execute(eq(UUID.fromString(OP)), eq(UUID.fromString(PAGE)), eq("es"), any()))
                .thenReturn(translation());

        mockMvc.perform(get("/api/tour-operators/{id}/pages/{pageId}/translations/{locale}", OP, PAGE, "es")
                        .header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("sobre-nosotros"))
                .andDo(document("page-translations/get",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token"))));
    }

    @Test
    void upsert() throws Exception {
        authenticated();

        mockMvc.perform(put("/api/tour-operators/{id}/pages/{pageId}/translations/{locale}", OP, PAGE, "es")
                        .with(csrf())
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Sobre nosotros\",\"body\":\"<p>Hola</p>\",\"slug\":\"sobre-nosotros\"}"))
                .andExpect(status().isNoContent())
                .andDo(document("page-translations/upsert",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        requestFields(
                                fieldWithPath("title").description("Translated title; blank/absent = untranslated").optional(),
                                fieldWithPath("body").description("Translated raw-HTML body; blank/absent = untranslated").optional(),
                                fieldWithPath("seoTitle").type("String").description("Translated SEO title; blank/absent = untranslated").optional(),
                                fieldWithPath("seoDescription").type("String").description("Translated SEO description; blank/absent = untranslated").optional(),
                                fieldWithPath("slug").description("Localized handle: explicit (taken → 409), absent with a translated title derives one, absent without = canonical serves the locale").optional())));
    }

    @Test
    void deleteOne() throws Exception {
        authenticated();

        mockMvc.perform(delete("/api/tour-operators/{id}/pages/{pageId}/translations/{locale}", OP, PAGE, "es")
                        .with(csrf())
                        .header("Authorization", BEARER))
                .andExpect(status().isNoContent())
                .andDo(document("page-translations/delete",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token"))));
    }
}

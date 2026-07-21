package com.vointika.reference.presentation.controller;

import com.vointika.reference.application.usecase.ListLanguagesUseCase;
import com.vointika.reference.domain.entity.Language;
import com.vointika.shared.port.AccessTokenValidatorPort;
import com.vointika.shared.web.security.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.http.HttpDocumentation.httpRequest;
import static org.springframework.restdocs.http.HttpDocumentation.httpResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LanguageController.class)
@ExtendWith(RestDocumentationExtension.class)
@Import(SecurityConfig.class)
class LanguageControllerDocumentationTest {

    private MockMvc mockMvc;

    @MockitoBean
    private ListLanguagesUseCase listLanguagesUseCase;

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
    void listLanguages() throws Exception {
        when(accessTokenValidator.isValid("test-access-token")).thenReturn(true);
        when(accessTokenValidator.extractUserId("test-access-token"))
                .thenReturn("550e8400-e29b-41d4-a716-446655440000");
        when(listLanguagesUseCase.execute()).thenReturn(List.of(
                new Language(UUID.fromString("dddd0001-0000-0000-0000-000000000001"), "en", "English"),
                new Language(UUID.fromString("dddd0002-0000-0000-0000-000000000002"), "es", "Spanish")));

        mockMvc.perform(get("/api/languages").header("Authorization", "Bearer test-access-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].context").value("languages"))
                .andExpect(jsonPath("$[0].code").value("en"))
                .andDo(document("languages/list",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        responseFields(
                                fieldWithPath("[].id").description("The language id"),
                                fieldWithPath("[].context").description("The entity's collection: \"languages\""),
                                fieldWithPath("[].code").description("The BCP-47 locale code (lowercase)"),
                                fieldWithPath("[].name").description("Display name"))));
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/languages")).andExpect(status().isUnauthorized());
    }
}

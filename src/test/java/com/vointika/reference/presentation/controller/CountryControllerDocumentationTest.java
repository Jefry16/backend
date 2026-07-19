package com.vointika.reference.presentation.controller;

import com.vointika.shared.media.MediaUrlResolver;
import com.vointika.shared.port.AccessTokenValidatorPort;
import com.vointika.shared.web.security.SecurityConfig;
import com.vointika.reference.application.usecase.ListCountriesUseCase;
import com.vointika.reference.domain.entity.Country;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CountryController.class)
@ExtendWith(RestDocumentationExtension.class)
@Import(SecurityConfig.class)
class CountryControllerDocumentationTest {

    private MockMvc mockMvc;

    @MockitoBean
    private ListCountriesUseCase listCountriesUseCase;

    @MockitoBean
    private AccessTokenValidatorPort accessTokenValidator;

    @MockitoBean
    private MediaUrlResolver mediaUrlResolver;


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

    @Test
    void listCountries() throws Exception {
        when(accessTokenValidator.isValid("test-access-token")).thenReturn(true);
        when(accessTokenValidator.extractUserId("test-access-token"))
                .thenReturn("550e8400-e29b-41d4-a716-446655440000");
        // Flag storage key → resolved URL at read time; null stays null.
        when(mediaUrlResolver.toUrl(any())).thenAnswer(inv -> {
            String key = inv.getArgument(0);
            return key == null ? null : "https://media.example/" + key;
        });
        when(listCountriesUseCase.execute()).thenReturn(List.of(
                new Country(UUID.fromString("33333333-3333-3333-3333-333333333333"),
                        "DO", "Dominican Republic", null),
                new Country(UUID.fromString("11111111-1111-1111-1111-111111111111"),
                        "ES", "Spain", "flags/es.svg")
        ));

        mockMvc.perform(get("/api/countries")
                        .header("Authorization", "Bearer test-access-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("countries"))
                .andExpect(jsonPath("$[0].code").value("DO"))
                .andExpect(jsonPath("$[1].flagUrl").value("https://media.example/flags/es.svg"))
                .andDo(document("countries/list",
                        requestHeaders(
                                headerWithName("Authorization").description("Bearer access token")
                        ),
                        responseFields(
                                fieldWithPath("[].id").description("The country's UUID"),
                                fieldWithPath("[].type").description("Resource type (always 'countries')"),
                                fieldWithPath("[].code").description("ISO 3166-1 alpha-2 country code (e.g. ES, US, DO)"),
                                fieldWithPath("[].name").description("Human-readable country name"),
                                fieldWithPath("[].flagUrl").description("Resolved URL of the country's flag image, or null if unset").optional()
                        )));
    }
}

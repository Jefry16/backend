package com.vointika.reference.presentation.controller;

import com.vointika.shared.media.MediaUrlResolver;
import com.vointika.shared.port.AccessTokenValidatorPort;
import com.vointika.shared.web.security.SecurityConfig;
import com.vointika.reference.application.usecase.ListTimezonesUseCase;
import com.vointika.reference.domain.entity.Country;
import com.vointika.reference.domain.entity.Timezone;
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

@WebMvcTest(TimezoneController.class)
@ExtendWith(RestDocumentationExtension.class)
@Import(SecurityConfig.class)
class TimezoneControllerDocumentationTest {

    private MockMvc mockMvc;

    @MockitoBean
    private ListTimezonesUseCase listTimezonesUseCase;

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
    void listTimezones() throws Exception {
        when(accessTokenValidator.isValid("test-access-token")).thenReturn(true);
        when(accessTokenValidator.extractUserId("test-access-token"))
                .thenReturn("550e8400-e29b-41d4-a716-446655440000");
        when(mediaUrlResolver.toUrl(any())).thenAnswer(inv -> {
            String key = inv.getArgument(0);
            return key == null ? null : "https://media.example/" + key;
        });
        Country spain = new Country(UUID.fromString("11111111-1111-1111-1111-111111111111"), "ES", "Spain", "flags/es.svg");
        when(listTimezonesUseCase.execute()).thenReturn(List.of(
                new Timezone(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                        "Africa/Ceuta", "Ceuta", spain),
                new Timezone(UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd"),
                        "Europe/Madrid", "Madrid", spain)
        ));

        mockMvc.perform(get("/api/timezones")
                        .header("Authorization", "Bearer test-access-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].type").value("timezones"))
                .andExpect(jsonPath("$[0].name").value("Africa/Ceuta"))
                .andExpect(jsonPath("$[0].country.code").value("ES"))
                .andExpect(jsonPath("$[0].country.flagUrl").value("https://media.example/flags/es.svg"))
                .andDo(document("timezones/list",
                        requestHeaders(
                                headerWithName("Authorization").description("Bearer access token")
                        ),
                        responseFields(
                                fieldWithPath("[].id").description("The timezone's UUID"),
                                fieldWithPath("[].type").description("Resource type (always 'timezones')"),
                                fieldWithPath("[].name").description("IANA timezone identifier (e.g. Europe/Madrid)"),
                                fieldWithPath("[].cityName").description("Human-readable city label"),
                                fieldWithPath("[].country.id").description("The country's UUID"),
                                fieldWithPath("[].country.type").description("Resource type (always 'countries')"),
                                fieldWithPath("[].country.code").description("ISO 3166-1 alpha-2 country code"),
                                fieldWithPath("[].country.name").description("Human-readable country name"),
                                fieldWithPath("[].country.flagUrl").description("Resolved URL of the country's flag image")
                        )));
    }
}

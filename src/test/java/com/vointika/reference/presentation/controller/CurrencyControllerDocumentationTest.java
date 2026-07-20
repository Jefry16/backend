package com.vointika.reference.presentation.controller;

import com.vointika.shared.port.AccessTokenValidatorPort;
import com.vointika.shared.web.security.SecurityConfig;
import com.vointika.reference.application.usecase.ListCurrenciesUseCase;
import com.vointika.reference.domain.entity.Currency;
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
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CurrencyController.class)
@ExtendWith(RestDocumentationExtension.class)
@Import(SecurityConfig.class)
class CurrencyControllerDocumentationTest {

    private MockMvc mockMvc;

    @MockitoBean
    private ListCurrenciesUseCase listCurrenciesUseCase;

    @MockitoBean
    private AccessTokenValidatorPort accessTokenValidator;


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
    void listCurrencies() throws Exception {
        when(accessTokenValidator.isValid("test-access-token")).thenReturn(true);
        when(accessTokenValidator.extractUserId("test-access-token"))
                .thenReturn("550e8400-e29b-41d4-a716-446655440000");
        when(listCurrenciesUseCase.execute()).thenReturn(List.of(
                new Currency(UUID.fromString("11111111-1111-1111-1111-111111111111"),
                        "DOP", "Dominican Peso", "RD$"),
                new Currency(UUID.fromString("22222222-2222-2222-2222-222222222222"),
                        "EUR", "Euro", "€"),
                new Currency(UUID.fromString("33333333-3333-3333-3333-333333333333"),
                        "USD", "US Dollar", "$")
        ));

        mockMvc.perform(get("/api/currencies")
                        .header("Authorization", "Bearer test-access-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("DOP"))
                .andExpect(jsonPath("$[0].type").value("currencies"))
                .andDo(document("currencies/list",
                        requestHeaders(
                                headerWithName("Authorization").description("Bearer access token")
                        ),
                        responseFields(
                                fieldWithPath("[].id").description("The currency's UUID"),
                                fieldWithPath("[].type").description("Resource type (always 'currencies')"),
                                fieldWithPath("[].code").description("ISO 4217 alpha-3 currency code (e.g. USD, EUR, DOP)"),
                                fieldWithPath("[].name").description("Human-readable currency name"),
                                fieldWithPath("[].symbol").description("Currency symbol for UI display")
                        )));
    }
}

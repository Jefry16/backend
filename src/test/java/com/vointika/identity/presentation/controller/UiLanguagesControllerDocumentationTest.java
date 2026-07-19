package com.vointika.identity.presentation.controller;

import com.vointika.identity.application.usecase.ListUiLanguagesUseCase;
import com.vointika.identity.infrastructure.security.IdentityPublicRoutes;
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

import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UiLanguagesController.class)
@ExtendWith(RestDocumentationExtension.class)
@Import({SecurityConfig.class, IdentityPublicRoutes.class})
class UiLanguagesControllerDocumentationTest {

    private MockMvc mockMvc;

    @MockitoBean
    private ListUiLanguagesUseCase listUiLanguagesUseCase;

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
    void listUiLanguages() throws Exception {
        when(listUiLanguagesUseCase.execute()).thenReturn(List.of("en", "es"));

        // Public route — no Authorization header.
        mockMvc.perform(get("/api/ui-languages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("en"))
                .andExpect(jsonPath("$[1]").value("es"))
                .andDo(document("ui-languages/list",
                        responseFields(
                                fieldWithPath("[]").description(
                                        "Supported admin-UI language codes (lowercase locale codes), in configured order; the first is the default. Labels are derived on the client via Intl.DisplayNames.")
                        )));
    }
}

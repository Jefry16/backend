package com.vointika.touroperator.presentation.controller;

import com.vointika.shared.port.AccessTokenValidatorPort;
import com.vointika.shared.web.security.SecurityConfig;
import com.vointika.touroperator.application.dto.output.CreateTourOperatorOutput;
import com.vointika.touroperator.application.usecase.CreateTourOperatorUseCase;
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

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.http.HttpDocumentation.httpRequest;
import static org.springframework.restdocs.http.HttpDocumentation.httpResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TourOperatorController.class)
@ExtendWith(RestDocumentationExtension.class)
@Import(SecurityConfig.class)
class TourOperatorControllerDocumentationTest {

    private MockMvc mockMvc;

    @MockitoBean
    private CreateTourOperatorUseCase createTourOperatorUseCase;

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
    void create() throws Exception {
        UUID newId = UUID.fromString("7f000001-0000-4000-8000-000000000001");
        when(accessTokenValidator.isValid("test-access-token")).thenReturn(true);
        when(accessTokenValidator.extractUserId("test-access-token"))
                .thenReturn("550e8400-e29b-41d4-a716-446655440000");
        when(createTourOperatorUseCase.execute(any()))
                .thenReturn(new CreateTourOperatorOutput(newId));

        mockMvc.perform(post("/api/tour-operators")
                        .header("Authorization", "Bearer test-access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Acme Tours",
                                    "address": "123 Beach Rd, Punta Cana",
                                    "timezoneId": "cccccccc-cccc-cccc-cccc-cccccccccccc",
                                    "currencyId": "cccc0001-0000-0000-0000-000000000001"
                                }"""))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/tour-operators/" + newId))
                .andDo(document("tour-operators/create",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        requestFields(
                                fieldWithPath("name").description("The operator's display name (2-150 chars)"),
                                fieldWithPath("address").description("The operator's postal address (<=500 chars)"),
                                fieldWithPath("timezoneId").description("A reference timezone id (GET /api/timezones)"),
                                fieldWithPath("currencyId").description("A reference currency id (GET /api/currencies)")),
                        responseHeaders(headerWithName("Location").description("URI of the created tour operator"))));
    }

    @Test
    void createRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/tour-operators")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Acme Tours",
                                    "address": "123 Beach Rd",
                                    "timezoneId": "cccccccc-cccc-cccc-cccc-cccccccccccc",
                                    "currencyId": "cccc0001-0000-0000-0000-000000000001"
                                }"""))
                .andExpect(status().isUnauthorized());
    }
}

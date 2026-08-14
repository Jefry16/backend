package com.vointika.metafield.presentation.controller;

import com.vointika.metafield.application.usecase.DeleteMetafieldValueUseCase;
import com.vointika.metafield.application.usecase.ListMetafieldValuesUseCase;
import com.vointika.metafield.application.usecase.UpsertMetafieldValueUseCase;
import com.vointika.metafield.domain.projection.MetafieldValueWithDefinition;
import com.vointika.metafield.domain.valueobject.MetafieldOwnerType;
import com.vointika.metafield.domain.valueobject.MetafieldType;
import com.vointika.shared.exception.ResourceNotFoundException;
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

import java.time.Instant;
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
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PageMetafieldController.class)
@ExtendWith(RestDocumentationExtension.class)
@Import({SecurityConfig.class, WebConfig.class, ListQueryParser.class})
class PageMetafieldControllerDocumentationTest {

    private static final String OP = "019f7f33-1833-7dc1-b008-47e6c68b3ea2";
    private static final String PAGE = "cccccccc-0000-4000-8000-000000000002";
    private static final String USER = "550e8400-e29b-41d4-a716-446655440000";
    private static final String TOKEN = "test-access-token";
    private static final String BEARER = "Bearer " + TOKEN;

    private MockMvc mockMvc;

    @MockitoBean private ListMetafieldValuesUseCase listUseCase;
    @MockitoBean private UpsertMetafieldValueUseCase upsertUseCase;
    @MockitoBean private DeleteMetafieldValueUseCase deleteUseCase;
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

    @Test
    void list() throws Exception {
        authenticated();
        when(listUseCase.execute(eq(UUID.fromString(OP)), eq(MetafieldOwnerType.PAGE),
                eq(UUID.fromString(PAGE)), any()))
                .thenReturn(List.of(new MetafieldValueWithDefinition(
                        "custom", "difficulty", MetafieldType.SINGLE_LINE_TEXT,
                        "Difficulty", "Moderate", Instant.parse("2026-07-27T10:00:00Z"))));

        mockMvc.perform(get("/api/tour-operators/{id}/pages/{pageId}/metafields", OP, PAGE)
                        .header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].value").value("Moderate"))
                .andDo(document("page-metafields/list",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        responseFields(
                                fieldWithPath("[].namespace").description("The definition's namespace"),
                                fieldWithPath("[].key").description("The definition's key"),
                                fieldWithPath("[].type").description("The value type code"),
                                fieldWithPath("[].name").description("The definition's display name"),
                                fieldWithPath("[].value").description("The stored canonical value"),
                                fieldWithPath("[].updatedAt").description("Last change"))));
    }

    @Test
    void upsert() throws Exception {
        authenticated();

        mockMvc.perform(put("/api/tour-operators/{id}/pages/{pageId}/metafields/{namespace}/{key}",
                        OP, PAGE, "custom", "difficulty")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\":\"Moderate\"}"))
                .andExpect(status().isNoContent())
                .andDo(document("page-metafields/upsert",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        requestFields(fieldWithPath("value")
                                .description("The raw value; validated + normalized against the definition's type (mismatch → 422). Blank → 422 — clearing is DELETE's job"))));
    }

    @Test
    void deleteOne() throws Exception {
        authenticated();

        mockMvc.perform(delete("/api/tour-operators/{id}/pages/{pageId}/metafields/{namespace}/{key}",
                        OP, PAGE, "custom", "difficulty")
                        .header("Authorization", BEARER))
                .andExpect(status().isNoContent())
                .andDo(document("page-metafields/delete",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token"))));
    }

    @Test
    void unknownDefinitionIs404() throws Exception {
        authenticated();
        org.mockito.Mockito.doThrow(new ResourceNotFoundException("Metafield definition not found"))
                .when(upsertUseCase).execute(any());

        mockMvc.perform(put("/api/tour-operators/{id}/pages/{pageId}/metafields/{namespace}/{key}",
                        OP, PAGE, "custom", "missing")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\":\"x\"}"))
                .andExpect(status().isNotFound());
    }
}

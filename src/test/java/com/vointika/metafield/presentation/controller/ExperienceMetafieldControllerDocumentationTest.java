package com.vointika.metafield.presentation.controller;

import com.vointika.metafield.domain.repository.MetafieldDefinitionRepository;
import com.vointika.shared.web.docs.ApiErrorSnippets;
import com.vointika.metafield.application.usecase.DeleteMetafieldValueUseCase;
import com.vointika.metafield.application.usecase.ListMetafieldValuesUseCase;
import com.vointika.metafield.application.usecase.UpsertMetafieldValueUseCase;
import com.vointika.metafield.domain.projection.MetafieldValueWithDefinition;
import com.vointika.metafield.domain.valueobject.MetafieldOwnerType;
import com.vointika.metafield.domain.valueobject.MetafieldType;
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
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExperienceMetafieldController.class)
@ExtendWith(RestDocumentationExtension.class)
@Import({SecurityConfig.class, WebConfig.class, ListQueryParser.class})
class ExperienceMetafieldControllerDocumentationTest {

    private static final String OP = "019f7f33-1833-7dc1-b008-47e6c68b3ea2";
    private static final String EXP = "cccccccc-0000-4000-8000-000000000002";
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
        when(listUseCase.execute(eq(UUID.fromString(OP)), eq(MetafieldOwnerType.EXPERIENCE),
                eq(UUID.fromString(EXP)), any()))
                .thenReturn(List.of(new MetafieldValueWithDefinition(
                        "custom", "difficulty", MetafieldType.SINGLE_LINE_TEXT,
                        "Difficulty", "Moderate", Instant.parse("2026-07-27T10:00:00Z"))));

        mockMvc.perform(get("/api/tour-operators/{id}/experiences/{experienceId}/metafields", OP, EXP)
                        .header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].value").value("Moderate"))
                .andDo(document("experience-metafields/list",
                        pathParameters(
                                parameterWithName("id").description(
                                        "The tour operator id"),
                                parameterWithName("experienceId").description(
                                        "The experience the value belongs to. One that does not exist, or belongs "
                                                + "to another operator, is a 404 — the two are indistinguishable")),
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

        mockMvc.perform(put("/api/tour-operators/{id}/experiences/{experienceId}/metafields/{namespace}/{key}",
                        OP, EXP, "custom", "difficulty")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\":\"Moderate\"}"))
                .andExpect(status().isNoContent())
                .andDo(document("experience-metafields/upsert",
                        pathParameters(
                                parameterWithName("id").description(
                                        "The tour operator id"),
                                parameterWithName("experienceId").description(
                                        "The experience the value belongs to. One that does not exist, or belongs "
                                                + "to another operator, is a 404 — the two are indistinguishable"),
                                parameterWithName("namespace").description(
                                        "The namespace half of the identifier. With {key} it names an EXISTING definition; an undefined pair is a 404 on every verb here"),
                                parameterWithName("key").description(
                                        "The key half of the identifier. Handle-shaped, and paired with {namespace} it is what the definition is looked up by")),
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        requestFields(fieldWithPath("value")
                                .description("The raw value; validated + normalized against the definition's type (mismatch → 422). Blank → 422 — clearing is DELETE's job"))));
    }

    @Test
    void deleteOne() throws Exception {
        authenticated();

        mockMvc.perform(delete("/api/tour-operators/{id}/experiences/{experienceId}/metafields/{namespace}/{key}",
                        OP, EXP, "custom", "difficulty")
                        .header("Authorization", BEARER))
                .andExpect(status().isNoContent())
                .andDo(document("experience-metafields/delete",
                        pathParameters(
                                parameterWithName("id").description(
                                        "The tour operator id"),
                                parameterWithName("experienceId").description(
                                        "The experience the value belongs to. One that does not exist, or belongs "
                                                + "to another operator, is a 404 — the two are indistinguishable"),
                                parameterWithName("namespace").description(
                                        "The namespace half of the identifier. With {key} it names an EXISTING definition; an undefined pair is a 404 on every verb here"),
                                parameterWithName("key").description(
                                        "The key half of the identifier. Handle-shaped, and paired with {namespace} it is what the definition is looked up by")),
                        requestHeaders(headerWithName("Authorization").description("Bearer access token"))));
    }

    @Test
    void unknownDefinitionIs404() throws Exception {
        authenticated();
        org.mockito.Mockito.doThrow(MetafieldDefinitionRepository.NOT_FOUND.get())
                .when(upsertUseCase).execute(any());

        mockMvc.perform(put("/api/tour-operators/{id}/experiences/{experienceId}/metafields/{namespace}/{key}",
                        OP, EXP, "custom", "missing")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\":\"x\"}"))
                .andExpect(status().isNotFound())
                .andDo(document("experience-metafields/upsert-not-found",
                        pathParameters(
                                parameterWithName("id").description("The tour operator id"),
                                parameterWithName("experienceId").description("The experience id"),
                                parameterWithName("namespace").description(
                                        "A namespace with no definition for this owner type"),
                                parameterWithName("key").description(
                                        "…and the key half. The pair must already be DEFINED — this endpoint never creates one")),
                        requestHeaders(headerWithName("Authorization")
                                .description("Bearer access token")),
                        requestFields(fieldWithPath("value").description(
                                "Still required — this is the ordinary request body. It is simply "
                                        + "never read on this path, because the definition is looked "
                                        + "up first")),
                        responseFields(ApiErrorSnippets.errorFields())));
    }
}

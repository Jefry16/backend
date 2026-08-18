package com.vointika.metafield.presentation.controller;

import com.vointika.shared.exception.ForbiddenException;
import com.vointika.shared.web.docs.ApiErrorSnippets;
import com.vointika.metafield.application.usecase.CreateMetafieldDefinitionUseCase;
import com.vointika.metafield.application.usecase.DeleteMetafieldDefinitionUseCase;
import com.vointika.metafield.application.usecase.GetMetafieldDefinitionUseCase;
import com.vointika.metafield.application.usecase.ListMetafieldDefinitionsUseCase;
import com.vointika.metafield.application.usecase.UpdateMetafieldDefinitionUseCase;
import com.vointika.metafield.domain.entity.MetafieldDefinition;
import com.vointika.metafield.domain.projection.MetafieldDefinitionListItem;
import com.vointika.metafield.domain.valueobject.MetafieldDefinitionName;
import com.vointika.metafield.domain.valueobject.MetafieldDescription;
import com.vointika.metafield.domain.valueobject.MetafieldKey;
import com.vointika.metafield.domain.valueobject.MetafieldNamespace;
import com.vointika.metafield.domain.valueobject.MetafieldOwnerType;
import com.vointika.metafield.domain.valueobject.MetafieldType;
import com.vointika.shared.exception.ResourceAlreadyExistsException;
import com.vointika.shared.list.CursorPage;
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
import org.springframework.restdocs.payload.JsonFieldType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MetafieldDefinitionController.class)
@ExtendWith(RestDocumentationExtension.class)
@Import({SecurityConfig.class, WebConfig.class, ListQueryParser.class})
class MetafieldDefinitionControllerDocumentationTest {

    private static final String OP = "019f7f33-1833-7dc1-b008-47e6c68b3ea2";
    private static final String DEF = "cccccccc-0000-4000-8000-000000000001";
    private static final String USER = "550e8400-e29b-41d4-a716-446655440000";
    private static final String STAFF_USER = "7c9e6679-7425-40de-944b-e07fc1f90ae7";
    private static final String TOKEN = "test-access-token";
    private static final String BEARER = "Bearer " + TOKEN;
    private static final String CREATE_BODY =
            "{\"ownerType\":\"page\",\"namespace\":\"custom\",\"key\":\"subtitle\","
                    + "\"type\":\"single_line_text\",\"name\":\"Subtitle\","
                    + "\"description\":\"Shown under the title\"}";

    private MockMvc mockMvc;

    @MockitoBean private CreateMetafieldDefinitionUseCase createUseCase;
    @MockitoBean private UpdateMetafieldDefinitionUseCase updateUseCase;
    @MockitoBean private GetMetafieldDefinitionUseCase getUseCase;
    @MockitoBean private ListMetafieldDefinitionsUseCase listUseCase;
    @MockitoBean private DeleteMetafieldDefinitionUseCase deleteUseCase;
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

    private MetafieldDefinition definition() {
        return new MetafieldDefinition(UUID.fromString(DEF), UUID.fromString(OP),
                MetafieldOwnerType.PAGE, new MetafieldNamespace("custom"),
                new MetafieldKey("subtitle"), MetafieldType.SINGLE_LINE_TEXT, null,
                new MetafieldDefinitionName("Subtitle"),
                new MetafieldDescription("Shown under the title"),
                UUID.fromString(USER),
                Instant.parse("2026-07-27T10:00:00Z"), Instant.parse("2026-07-27T10:00:00Z"));
    }

    @Test
    void create() throws Exception {
        authenticated();
        when(createUseCase.execute(any())).thenReturn(UUID.fromString(DEF));

        mockMvc.perform(post("/api/tour-operators/{id}/metafield-definitions", OP)
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andDo(document("metafield-definitions/create",
                        pathParameters(
                                parameterWithName("id").description(
                                        "The tour operator id")),
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        requestFields(
                                fieldWithPath("ownerType").description(
                                        "Which resource the definition applies to: "
                                                + MetafieldOwnerType.codes() + " (immutable)"),
                                fieldWithPath("namespace").description("Handle-shaped namespace half of the identifier (immutable)"),
                                fieldWithPath("key").description("Handle-shaped key half; namespace.key unique per (operator, owner type) — duplicate → 409 (immutable)"),
                                fieldWithPath("type").description(
                                        MetafieldType.codes() + " (immutable). It decides how the "
                                                + "value is validated on every write, and only "
                                                + MetafieldType.translatableCodes()
                                                + " can carry per-locale translations"),
                                fieldWithPath("metaobjectDefinitionId").type("String").description("Required iff type is metaobject_reference: the pinned metaobject type").optional(),
                                fieldWithPath("name").description("Display name (1–120)"),
                                fieldWithPath("description").description("Optional help text (≤500)").optional())));
    }

    @Test
    void createDuplicateIs409() throws Exception {
        authenticated();
        when(createUseCase.execute(any())).thenThrow(
                new ResourceAlreadyExistsException(CreateMetafieldDefinitionUseCase.DUPLICATE_IDENTITY));

        mockMvc.perform(post("/api/tour-operators/{id}/metafield-definitions", OP)
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ownerType\":\"page\",\"namespace\":\"custom\",\"key\":\"subtitle\","
                                + "\"type\":\"multi_line_text\",\"name\":\"Subtitle (long)\"}"))
                .andExpect(status().isConflict())
                .andDo(document("metafield-definitions/create-conflict",
                        pathParameters(parameterWithName("id").description("The tour operator id")),
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        responseFields(ApiErrorSnippets.errorFields())));
    }

    @Test
    void list() throws Exception {
        authenticated();
        when(listUseCase.execute(any(), any())).thenReturn(new CursorPage<>(
                List.of(new MetafieldDefinitionListItem(UUID.fromString(DEF),
                        MetafieldOwnerType.PAGE, "custom", "subtitle",
                        MetafieldType.SINGLE_LINE_TEXT, null, "Subtitle",
                        Instant.parse("2026-07-27T10:00:00Z"))),
                null));

        mockMvc.perform(get("/api/tour-operators/{id}/metafield-definitions", OP)
                        .header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].context").value("metafield-definitions"))
                .andDo(document("metafield-definitions/list",
                        pathParameters(
                                parameterWithName("id").description(
                                        "The tour operator id")),
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        responseFields(
                                fieldWithPath("data[].id").description("The definition id"),
                                fieldWithPath("data[].context").description("\"metafield-definitions\""),
                                fieldWithPath("data[].ownerType").description(
                                        "Which resource kind: " + MetafieldOwnerType.codes()),
                                fieldWithPath("data[].namespace").description("Namespace half of the identifier"),
                                fieldWithPath("data[].key").description("Key half of the identifier"),
                                fieldWithPath("data[].type").description("The value type code"),
                                fieldWithPath("data[].metaobjectDefinitionId").type("String").description("The pinned metaobject type (metaobject_reference only), or null").optional(),
                                fieldWithPath("data[].name").description("Display name"),
                                fieldWithPath("data[].createdAt").description("When created"),
                                fieldWithPath("nextCursor").type(JsonFieldType.STRING).description("Opaque cursor; null on the last page").optional())));
    }

    /**
     * <b>The role line for this whole section, published once.</b> Every read here
     * is member-visible and every write is ADMIN+ — definitions, metaobjects and
     * values alike. A STAFF member browses the operator's schema and its data and
     * changes none of it.
     */
    @Test
    void aStaffMemberCannotDefineAMetafield() throws Exception {
        when(accessTokenValidator.isValid("staff-access-token")).thenReturn(true);
        when(accessTokenValidator.extractUserId("staff-access-token")).thenReturn(STAFF_USER);
        doThrow(new ForbiddenException("This action requires ADMIN privileges"))
                .when(createUseCase).execute(any());

        mockMvc.perform(post("/api/tour-operators/{id}/metafield-definitions", OP)
                        .header("Authorization", "Bearer staff-access-token")
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isForbidden())
                .andDo(document("metafield-definitions/create-forbidden",
                        pathParameters(parameterWithName("id").description("The tour operator id")),
                        requestHeaders(headerWithName("Authorization").description(
                                "Bearer access token for a STAFF member — this error is about who asks, "
                                        + "so the URL and the body are the successful call's")),
                        responseFields(ApiErrorSnippets.errorFields())));
    }

    @Test
    void getOne() throws Exception {
        authenticated();
        when(getUseCase.execute(eq(UUID.fromString(OP)), eq(UUID.fromString(DEF)), any()))
                .thenReturn(definition());

        mockMvc.perform(get("/api/tour-operators/{id}/metafield-definitions/{definitionId}", OP, DEF)
                        .header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.namespace").value("custom"))
                .andDo(document("metafield-definitions/get",
                        pathParameters(
                                parameterWithName("id").description(
                                        "The tour operator id"),
                                parameterWithName("definitionId").description(
                                        "The definition id")),
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        responseFields(
                                fieldWithPath("id").description("The definition id"),
                                fieldWithPath("context").description("\"metafield-definitions\""),
                                fieldWithPath("ownerType").description(
                                        "Which resource kind: " + MetafieldOwnerType.codes()),
                                fieldWithPath("namespace").description("Namespace half of the identifier"),
                                fieldWithPath("key").description("Key half of the identifier"),
                                fieldWithPath("type").description(
                                        "The value type code, one of " + MetafieldType.codes()),
                                fieldWithPath("metaobjectDefinitionId").type(JsonFieldType.STRING)
                                        .description("The pinned metaobject type (metaobject_reference only), "
                                                + "or null").optional(),
                                fieldWithPath("name").description("Display name"),
                                fieldWithPath("description").type(JsonFieldType.STRING)
                                        .description("Help text for the operator's own editors, or null. "
                                                + "The list projection omits this and updatedAt").optional(),
                                fieldWithPath("createdAt").description("When created"),
                                fieldWithPath("updatedAt").description("When the name or description last changed"))));
    }

    @Test
    void update() throws Exception {
        authenticated();

        mockMvc.perform(put("/api/tour-operators/{id}/metafield-definitions/{definitionId}", OP, DEF)
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Sub-heading\",\"description\":null}"))
                .andExpect(status().isNoContent())
                .andDo(document("metafield-definitions/update",
                        pathParameters(
                                parameterWithName("id").description(
                                        "The tour operator id"),
                                parameterWithName("definitionId").description(
                                        "The definition id")),
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        requestFields(
                                fieldWithPath("name").description("Display name (1–120)"),
                                fieldWithPath("description").type("String").description("Help text; null/blank clears").optional())));
    }

    @Test
    void deleteOne() throws Exception {
        authenticated();

        mockMvc.perform(delete("/api/tour-operators/{id}/metafield-definitions/{definitionId}", OP, DEF)
                        .header("Authorization", BEARER))
                .andExpect(status().isNoContent())
                .andDo(document("metafield-definitions/delete",
                        pathParameters(
                                parameterWithName("id").description(
                                        "The tour operator id"),
                                parameterWithName("definitionId").description(
                                        "The definition id")),
                        requestHeaders(headerWithName("Authorization").description("Bearer access token"))));
    }
}

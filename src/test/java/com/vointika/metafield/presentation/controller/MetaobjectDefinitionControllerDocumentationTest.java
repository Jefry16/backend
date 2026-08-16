package com.vointika.metafield.presentation.controller;

import com.vointika.metafield.application.dto.output.MetaobjectDefinitionView;
import com.vointika.metafield.application.usecase.AddMetaobjectFieldUseCase;
import com.vointika.metafield.application.usecase.CreateMetaobjectDefinitionUseCase;
import com.vointika.metafield.application.usecase.DeleteMetaobjectDefinitionUseCase;
import com.vointika.metafield.application.usecase.GetMetaobjectDefinitionUseCase;
import com.vointika.metafield.application.usecase.ListMetaobjectDefinitionsUseCase;
import com.vointika.metafield.application.usecase.RemoveMetaobjectFieldUseCase;
import com.vointika.metafield.application.usecase.RenameMetaobjectFieldUseCase;
import com.vointika.metafield.application.usecase.UpdateMetaobjectDefinitionUseCase;
import com.vointika.metafield.domain.entity.MetaobjectDefinition;
import com.vointika.metafield.domain.entity.MetaobjectField;
import com.vointika.metafield.domain.projection.MetaobjectDefinitionListItem;
import com.vointika.metafield.domain.valueobject.MetafieldDefinitionName;
import com.vointika.metafield.domain.valueobject.MetafieldDescription;
import com.vointika.metafield.domain.valueobject.MetafieldKey;
import com.vointika.metafield.domain.valueobject.MetafieldType;
import com.vointika.metafield.domain.valueobject.MetaobjectType;
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
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.patch;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MetaobjectDefinitionController.class)
@ExtendWith(RestDocumentationExtension.class)
@Import({SecurityConfig.class, WebConfig.class, ListQueryParser.class})
class MetaobjectDefinitionControllerDocumentationTest {

    private static final String OP = "019f7f33-1833-7dc1-b008-47e6c68b3ea2";
    private static final String DEF = "dddddddd-0000-4000-8000-000000000001";
    private static final String USER = "550e8400-e29b-41d4-a716-446655440000";
    private static final String TOKEN = "test-access-token";
    private static final String BEARER = "Bearer " + TOKEN;
    private static final String CREATE_BODY =
            "{\"type\":\"size-chart\",\"name\":\"Size chart\","
                    + "\"description\":\"Sizing guidance shown on tours\","
                    + "\"fields\":[{\"key\":\"heading\",\"type\":\"single_line_text\",\"name\":\"Heading\"},"
                    + "{\"key\":\"rows\",\"type\":\"json\",\"name\":\"Rows\"}]}";

    private MockMvc mockMvc;

    @MockitoBean private CreateMetaobjectDefinitionUseCase createUseCase;
    @MockitoBean private UpdateMetaobjectDefinitionUseCase updateUseCase;
    @MockitoBean private GetMetaobjectDefinitionUseCase getUseCase;
    @MockitoBean private ListMetaobjectDefinitionsUseCase listUseCase;
    @MockitoBean private DeleteMetaobjectDefinitionUseCase deleteUseCase;
    @MockitoBean private AddMetaobjectFieldUseCase addFieldUseCase;
    @MockitoBean private RenameMetaobjectFieldUseCase renameFieldUseCase;
    @MockitoBean private RemoveMetaobjectFieldUseCase removeFieldUseCase;
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

    private MetaobjectDefinitionView view() {
        MetaobjectDefinition definition = new MetaobjectDefinition(
                UUID.fromString(DEF), UUID.fromString(OP), new MetaobjectType("size-chart"),
                new MetafieldDefinitionName("Size chart"),
                new MetafieldDescription("Sizing guidance shown on tours"),
                UUID.fromString(USER),
                Instant.parse("2026-07-28T10:00:00Z"), Instant.parse("2026-07-28T10:00:00Z"));
        return new MetaobjectDefinitionView(definition, List.of(
                new MetaobjectField(UUID.randomUUID(), definition.getId(),
                        new MetafieldKey("heading"), MetafieldType.SINGLE_LINE_TEXT,
                        new MetafieldDefinitionName("Heading"), 1,
                        Instant.parse("2026-07-28T10:00:00Z"), Instant.parse("2026-07-28T10:00:00Z")),
                new MetaobjectField(UUID.randomUUID(), definition.getId(),
                        new MetafieldKey("rows"), MetafieldType.JSON,
                        new MetafieldDefinitionName("Rows"), 2,
                        Instant.parse("2026-07-28T10:00:00Z"), Instant.parse("2026-07-28T10:00:00Z"))));
    }

    @Test
    void create() throws Exception {
        authenticated();
        when(createUseCase.execute(any())).thenReturn(UUID.fromString(DEF));

        mockMvc.perform(post("/api/tour-operators/{id}/metaobject-definitions", OP)
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andDo(document("metaobject-definitions/create",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        requestFields(
                                fieldWithPath("type").description("Handle-shaped type identifier, unique per operator — duplicate → 409 (immutable)"),
                                fieldWithPath("name").description("Display name (1–120)"),
                                fieldWithPath("description").description("Optional help text (≤500)").optional(),
                                fieldWithPath("fields[]").description("The initial fields in order (at least one)"),
                                fieldWithPath("fields[].key").description("Handle-shaped key, unique within the definition (immutable)"),
                                fieldWithPath("fields[].type").description("single_line_text | multi_line_text | number_integer | number_decimal | boolean | date | url | json (immutable)"),
                                fieldWithPath("fields[].name").description("The field's display name (1–120)"))));
    }

    @Test
    void createDuplicateTypeIs409() throws Exception {
        authenticated();
        when(createUseCase.execute(any())).thenThrow(
                new ResourceAlreadyExistsException("A metaobject definition with this type already exists"));

        mockMvc.perform(post("/api/tour-operators/{id}/metaobject-definitions", OP)
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isConflict());
    }

    @Test
    void list() throws Exception {
        authenticated();
        when(listUseCase.execute(any(), any())).thenReturn(new CursorPage<>(
                List.of(new MetaobjectDefinitionListItem(UUID.fromString(DEF),
                        new MetaobjectType("size-chart"), "Size chart",
                        Instant.parse("2026-07-28T10:00:00Z"))),
                null));

        mockMvc.perform(get("/api/tour-operators/{id}/metaobject-definitions", OP)
                        .header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].context").value("metaobject-definitions"))
                .andDo(document("metaobject-definitions/list",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        responseFields(
                                fieldWithPath("data[].id").description("The definition id"),
                                fieldWithPath("data[].context").description("\"metaobject-definitions\""),
                                fieldWithPath("data[].type").description("The type identifier"),
                                fieldWithPath("data[].name").description("Display name"),
                                fieldWithPath("data[].createdAt").description("When created"),
                                fieldWithPath("nextCursor").type(JsonFieldType.STRING).description("Opaque cursor; null on the last page").optional())));
    }

    @Test
    void getOne() throws Exception {
        authenticated();
        when(getUseCase.execute(eq(UUID.fromString(OP)), eq(UUID.fromString(DEF)), any()))
                .thenReturn(view());

        mockMvc.perform(get("/api/tour-operators/{id}/metaobject-definitions/{definitionId}", OP, DEF)
                        .header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fields[0].key").value("heading"))
                .andDo(document("metaobject-definitions/get",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        responseFields(
                                fieldWithPath("id").description("The definition id"),
                                fieldWithPath("context").description("\"metaobject-definitions\""),
                                fieldWithPath("type").description("The type identifier (immutable)"),
                                fieldWithPath("name").description("Display name"),
                                fieldWithPath("description").type("String").description("Help text, or null").optional(),
                                fieldWithPath("fields[]").description("The fields in position order"),
                                fieldWithPath("fields[].key").description("The field key (immutable)"),
                                fieldWithPath("fields[].type").description("The field's value type code (immutable)"),
                                fieldWithPath("fields[].name").description("The field's display name"),
                                fieldWithPath("createdAt").description("When created"),
                                fieldWithPath("updatedAt").description("When last changed"))));
    }

    @Test
    void update() throws Exception {
        authenticated();

        mockMvc.perform(put("/api/tour-operators/{id}/metaobject-definitions/{definitionId}", OP, DEF)
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Sizing chart\",\"description\":null}"))
                .andExpect(status().isNoContent())
                .andDo(document("metaobject-definitions/update",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        requestFields(
                                fieldWithPath("name").description("Display name (1–120)"),
                                fieldWithPath("description").type("String").description("Help text; null/blank clears").optional())));
    }

    @Test
    void deleteOne() throws Exception {
        authenticated();

        mockMvc.perform(delete("/api/tour-operators/{id}/metaobject-definitions/{definitionId}", OP, DEF)
                        .header("Authorization", BEARER))
                .andExpect(status().isNoContent())
                .andDo(document("metaobject-definitions/delete",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token"))));
    }

    @Test
    void addField() throws Exception {
        authenticated();

        mockMvc.perform(post("/api/tour-operators/{id}/metaobject-definitions/{definitionId}/fields", OP, DEF)
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"key\":\"note\",\"type\":\"multi_line_text\",\"name\":\"Note\"}"))
                .andExpect(status().isNoContent())
                .andDo(document("metaobject-definitions/add-field",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        requestFields(
                                fieldWithPath("key").description("Handle-shaped key, unique within the definition — duplicate → 409 (immutable)"),
                                fieldWithPath("type").description("The value type code (immutable)"),
                                fieldWithPath("name").description("The field's display name (1–120)"))));
    }

    @Test
    void renameField() throws Exception {
        authenticated();

        mockMvc.perform(patch("/api/tour-operators/{id}/metaobject-definitions/{definitionId}/fields/{key}",
                        OP, DEF, "heading")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Header\"}"))
                .andExpect(status().isNoContent())
                .andDo(document("metaobject-definitions/rename-field",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        requestFields(
                                fieldWithPath("name").description("The field's new display name (1–120)"))));
    }

    @Test
    void removeField() throws Exception {
        authenticated();

        mockMvc.perform(delete("/api/tour-operators/{id}/metaobject-definitions/{definitionId}/fields/{key}",
                        OP, DEF, "rows")
                        .header("Authorization", BEARER))
                .andExpect(status().isNoContent())
                .andDo(document("metaobject-definitions/remove-field",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token"))));
    }
}

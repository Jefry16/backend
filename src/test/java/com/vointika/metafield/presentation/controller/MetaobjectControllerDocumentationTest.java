package com.vointika.metafield.presentation.controller;

import com.vointika.shared.web.docs.ApiErrorSnippets;
import com.vointika.metafield.application.dto.output.MetaobjectEntryView;
import com.vointika.metafield.application.usecase.CreateMetaobjectEntryUseCase;
import com.vointika.metafield.application.usecase.DeleteMetaobjectEntryUseCase;
import com.vointika.metafield.application.usecase.GetMetaobjectEntryUseCase;
import com.vointika.metafield.application.usecase.ListMetaobjectEntriesUseCase;
import com.vointika.metafield.application.usecase.PublishMetaobjectEntryUseCase;
import com.vointika.metafield.application.usecase.UnpublishMetaobjectEntryUseCase;
import com.vointika.metafield.application.usecase.UpdateMetaobjectEntryUseCase;
import com.vointika.metafield.domain.entity.MetaobjectEntry;
import com.vointika.metafield.domain.projection.MetaobjectEntryListItem;
import com.vointika.metafield.domain.valueobject.MetaobjectEntryName;
import com.vointika.shared.exception.ConflictException;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.port.AccessTokenValidatorPort;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.valueobject.Handle;
import com.vointika.shared.web.list.ListQueryParser;
import com.vointika.shared.web.security.SecurityConfig;
import com.vointika.touroperator.infrastructure.web.WebConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
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
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MetaobjectController.class)
@ExtendWith(RestDocumentationExtension.class)
@Import({SecurityConfig.class, WebConfig.class, ListQueryParser.class})
class MetaobjectControllerDocumentationTest {

    private static final String OP = "019f7f33-1833-7dc1-b008-47e6c68b3ea2";
    private static final String DEF = "dddddddd-0000-4000-8000-000000000001";
    private static final String ENTRY = "eeeeeeee-0000-4000-8000-000000000001";
    /** A DIFFERENT entry from {@link #ENTRY} — this 409 turns on which one is already live. */
    private static final String LIVE_ENTRY = "eeeeeeee-0000-4000-8000-0000000000ff";
    private static final String DRAFT_ENTRY = "eeeeeeee-0000-4000-8000-0000000000fe";
    private static final String USER = "550e8400-e29b-41d4-a716-446655440000";
    private static final String TOKEN = "test-access-token";
    private static final String BEARER = "Bearer " + TOKEN;

    private MockMvc mockMvc;

    @MockitoBean private CreateMetaobjectEntryUseCase createUseCase;
    @MockitoBean private UpdateMetaobjectEntryUseCase updateUseCase;
    @MockitoBean private GetMetaobjectEntryUseCase getUseCase;
    @MockitoBean private ListMetaobjectEntriesUseCase listUseCase;
    @MockitoBean private DeleteMetaobjectEntryUseCase deleteUseCase;
    @MockitoBean private PublishMetaobjectEntryUseCase publishUseCase;
    @MockitoBean private UnpublishMetaobjectEntryUseCase unpublishUseCase;
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

    private MetaobjectEntryView view() {
        MetaobjectEntry entry = new MetaobjectEntry(
                UUID.fromString(ENTRY), UUID.fromString(OP), UUID.fromString(DEF),
                new Handle("beginner-chart"), new MetaobjectEntryName("Beginner chart"), false,
                UUID.fromString(USER),
                Instant.parse("2026-07-28T10:00:00Z"), Instant.parse("2026-07-28T10:00:00Z"));
        return new MetaobjectEntryView(entry, List.of(
                new MetaobjectEntryView.EntryFieldValue(
                        "heading", "single_line_text", "Heading", "Pick your kayak size"),
                new MetaobjectEntryView.EntryFieldValue("rows", "json", "Rows", null)));
    }

    @Test
    void create() throws Exception {
        authenticated();
        when(createUseCase.execute(any())).thenReturn(UUID.fromString(ENTRY));

        mockMvc.perform(post("/api/tour-operators/{id}/metaobjects", OP)
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"definitionId\":\"" + DEF + "\",\"handle\":\"beginner-chart\","
                                + "\"name\":\"Beginner chart\","
                                + "\"values\":{\"heading\":\"Pick your kayak size\"}}"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andDo(document("metaobjects/create",
                        pathParameters(
                                parameterWithName("id").description(
                                        "The tour operator id")),
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        requestFields(
                                fieldWithPath("definitionId").description("The definition (type) this entry belongs to"),
                                fieldWithPath("handle").description("Handle-shaped handle, unique per definition — duplicate → 409"),
                                fieldWithPath("name").description("Display name (1–120)"),
                                subsectionWithPath("values").description("field key → raw value; unknown key → 422, bad value → 422, null/blank stays unset").optional())));
    }

    @Test
    void list() throws Exception {
        authenticated();
        when(listUseCase.execute(any(), any())).thenReturn(new CursorPage<>(
                List.of(new MetaobjectEntryListItem(UUID.fromString(ENTRY), UUID.fromString(DEF),
                        "beginner-chart", "Beginner chart", true,
                        Instant.parse("2026-07-28T10:00:00Z"))),
                null));

        mockMvc.perform(get("/api/tour-operators/{id}/metaobjects", OP)
                        .queryParam("filter[definitionId][in]", DEF)
                        .header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].context").value("metaobjects"))
                .andDo(document("metaobjects/list",
                        pathParameters(
                                parameterWithName("id").description(
                                        "The tour operator id")),
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        responseFields(
                                fieldWithPath("data[].id").description("The entry id"),
                                fieldWithPath("data[].context").description("The entity's collection: \"metaobjects\""),
                                fieldWithPath("data[].definitionId").description("The definition this entry belongs to"),
                                fieldWithPath("data[].handle").description("The entry's handle"),
                                fieldWithPath("data[].name").description("Display name"),
                                fieldWithPath("data[].published").description("Whether the entry is live"),
                                fieldWithPath("data[].createdAt").description("When created"),
                                fieldWithPath("nextCursor").type(JsonFieldType.STRING).description("Opaque cursor; null on the last page").optional())));
    }

    @Test
    void getOne() throws Exception {
        authenticated();
        when(getUseCase.execute(eq(UUID.fromString(OP)), eq(UUID.fromString(ENTRY)), any()))
                .thenReturn(view());

        mockMvc.perform(get("/api/tour-operators/{id}/metaobjects/{metaobjectId}", OP, ENTRY)
                        .header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fields[0].value").value("Pick your kayak size"))
                .andDo(document("metaobjects/get",
                        pathParameters(
                                parameterWithName("id").description(
                                        "The tour operator id"),
                                parameterWithName("metaobjectId").description(
                                        "The metaobject entry id")),
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        responseFields(
                                fieldWithPath("id").description("The entry id"),
                                fieldWithPath("context").description("The entity's collection: \"metaobjects\""),
                                fieldWithPath("definitionId").description("The definition this entry belongs to"),
                                fieldWithPath("handle").description("The entry's handle"),
                                fieldWithPath("name").description("Display name"),
                                fieldWithPath("published").description("Whether the entry is live"),
                                fieldWithPath("fields[]").description("EVERY definition field in position order"),
                                fieldWithPath("fields[].key").description("The field key"),
                                fieldWithPath("fields[].type").description("The field's value type code"),
                                fieldWithPath("fields[].name").description("The field's display name"),
                                fieldWithPath("fields[].value").type("String").description("The stored value, or null when unset").optional(),
                                fieldWithPath("createdAt").description("When created"),
                                fieldWithPath("updatedAt").description("When last changed"))));
    }

    @Test
    void update() throws Exception {
        authenticated();

        mockMvc.perform(patch("/api/tour-operators/{id}/metaobjects/{metaobjectId}", OP, ENTRY)
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Starter chart\",\"values\":{\"rows\":null}}"))
                .andExpect(status().isNoContent())
                .andDo(document("metaobjects/update",
                        pathParameters(
                                parameterWithName("id").description(
                                        "The tour operator id"),
                                parameterWithName("metaobjectId").description(
                                        "The metaobject entry id")),
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        requestFields(
                                fieldWithPath("name").type("String").description("New display name; absent/null keeps current").optional(),
                                fieldWithPath("handle").type("String").description("New handle (unique per definition — duplicate → 409); absent/null keeps current").optional(),
                                subsectionWithPath("values").description("field key → value; null/blank clears the field, anything else replaces it; untouched keys keep their value").optional())));
    }

    @Test
    void publish() throws Exception {
        authenticated();

        mockMvc.perform(post("/api/tour-operators/{id}/metaobjects/{metaobjectId}/publish", OP, ENTRY)
                        .header("Authorization", BEARER))
                .andExpect(status().isNoContent())
                .andDo(document("metaobjects/publish",
                        pathParameters(
                                parameterWithName("id").description(
                                        "The tour operator id"),
                                parameterWithName("metaobjectId").description(
                                        "The metaobject entry id")),
                        requestHeaders(headerWithName("Authorization").description("Bearer access token"))));
    }

    @Test
    void publishTwiceIs409() throws Exception {
        authenticated();
        Mockito.doThrow(new ConflictException(MetaobjectEntry.ALREADY_PUBLISHED))
                .when(publishUseCase).execute(any(), any(), any());

        mockMvc.perform(post("/api/tour-operators/{id}/metaobjects/{metaobjectId}/publish", OP, LIVE_ENTRY)
                        .header("Authorization", BEARER))
                .andExpect(status().isConflict())
                .andDo(document("metaobjects/publish-conflict",
                        pathParameters(parameterWithName("id").description("The tour operator id"),
                                parameterWithName("metaobjectId").description(
                                        "An entry that is already published")),
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        responseFields(ApiErrorSnippets.errorFields())));
    }

    /**
     * <b>The mirror of {@code publishTwiceIs409}.</b> Both transitions are guarded
     * by the same entity, four lines apart — {@code MetaobjectEntry:78} and
     * {@code :86} — so documenting one and not the other tells a client the pair is
     * asymmetric when it is not. Only publish had an assertion, and publishing only
     * what was already asserted is how the gap survived the audit.
     */
    @Test
    void unpublishingWhatIsNotLiveIs409() throws Exception {
        authenticated();
        Mockito.doThrow(new ConflictException(MetaobjectEntry.NOT_PUBLISHED))
                .when(unpublishUseCase).execute(any(), any(), any());

        mockMvc.perform(post("/api/tour-operators/{id}/metaobjects/{metaobjectId}/unpublish", OP, DRAFT_ENTRY)
                        .header("Authorization", BEARER))
                .andExpect(status().isConflict())
                .andDo(document("metaobjects/unpublish-conflict",
                        pathParameters(parameterWithName("id").description("The tour operator id"),
                                parameterWithName("metaobjectId").description(
                                        "An entry that is still a draft")),
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        responseFields(ApiErrorSnippets.errorFields())));
    }

    @Test
    void unpublish() throws Exception {
        authenticated();

        mockMvc.perform(post("/api/tour-operators/{id}/metaobjects/{metaobjectId}/unpublish", OP, ENTRY)
                        .header("Authorization", BEARER))
                .andExpect(status().isNoContent())
                .andDo(document("metaobjects/unpublish",
                        pathParameters(
                                parameterWithName("id").description(
                                        "The tour operator id"),
                                parameterWithName("metaobjectId").description(
                                        "The metaobject entry id")),
                        requestHeaders(headerWithName("Authorization").description("Bearer access token"))));
    }

    @Test
    void deleteOne() throws Exception {
        authenticated();

        mockMvc.perform(delete("/api/tour-operators/{id}/metaobjects/{metaobjectId}", OP, ENTRY)
                        .header("Authorization", BEARER))
                .andExpect(status().isNoContent())
                .andDo(document("metaobjects/delete",
                        pathParameters(
                                parameterWithName("id").description(
                                        "The tour operator id"),
                                parameterWithName("metaobjectId").description(
                                        "The metaobject entry id")),
                        requestHeaders(headerWithName("Authorization").description("Bearer access token"))));
    }
}

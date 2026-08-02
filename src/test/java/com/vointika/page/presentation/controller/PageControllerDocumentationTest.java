package com.vointika.page.presentation.controller;

import com.vointika.page.application.usecase.CreatePageUseCase;
import com.vointika.page.application.usecase.DeletePageUseCase;
import com.vointika.page.application.usecase.GetPageUseCase;
import com.vointika.page.application.usecase.ListPagesUseCase;
import com.vointika.page.application.usecase.PublishPageUseCase;
import com.vointika.page.application.usecase.RenamePageUseCase;
import com.vointika.page.application.usecase.UnpublishPageUseCase;
import com.vointika.page.application.usecase.UpdatePageUseCase;
import com.vointika.page.domain.entity.Page;
import com.vointika.page.domain.enums.PageStatus;
import com.vointika.page.domain.projection.PageListItem;
import com.vointika.page.domain.valueobject.PageBody;
import com.vointika.page.domain.valueobject.PageSeoDescription;
import com.vointika.page.domain.valueobject.PageSeoTitle;
import com.vointika.page.domain.valueobject.PageTitle;
import com.vointika.shared.exception.ResourceAlreadyExistsException;
import com.vointika.shared.exception.ResourceNotFoundException;
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
import static org.mockito.Mockito.doThrow;
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
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PageController.class)
@ExtendWith(RestDocumentationExtension.class)
@Import({SecurityConfig.class, WebConfig.class, ListQueryParser.class})
class PageControllerDocumentationTest {

    private static final String OP = "019f7f33-1833-7dc1-b008-47e6c68b3ea2";
    private static final String PAGE = "bbbbbbbb-0000-4000-8000-000000000001";
    private static final String USER = "550e8400-e29b-41d4-a716-446655440000";
    private static final String TOKEN = "test-access-token";
    private static final String BEARER = "Bearer " + TOKEN;
    private static final String CREATE_BODY =
            "{\"title\":\"About us\",\"handle\":\"about-us\",\"body\":\"<p>Hello</p>\","
                    + "\"seoTitle\":\"About\",\"seoDescription\":\"Who we are\"}";

    private MockMvc mockMvc;

    @MockitoBean private CreatePageUseCase createPageUseCase;
    @MockitoBean private UpdatePageUseCase updatePageUseCase;
    @MockitoBean private ListPagesUseCase listPagesUseCase;
    @MockitoBean private GetPageUseCase getPageUseCase;
    @MockitoBean private PublishPageUseCase publishPageUseCase;
    @MockitoBean private UnpublishPageUseCase unpublishPageUseCase;
    @MockitoBean private RenamePageUseCase renamePageUseCase;
    @MockitoBean private DeletePageUseCase deletePageUseCase;
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

    private Page page() {
        return new Page(UUID.fromString(PAGE), UUID.fromString(OP),
                new PageTitle("About us"), new Handle("about-us"),
                new PageBody("<p>Hello</p>"),
                new PageSeoTitle("About"), new PageSeoDescription("Who we are"),
                PageStatus.DRAFT, null,
                UUID.fromString(USER),
                Instant.parse("2026-07-27T10:00:00Z"), Instant.parse("2026-07-27T10:00:00Z"));
    }

    @Test
    void list() throws Exception {
        authenticated();
        when(listPagesUseCase.execute(any(), any())).thenReturn(new CursorPage<>(
                List.of(new PageListItem(UUID.fromString(PAGE), "About us", "about-us",
                        PageStatus.DRAFT,
                        Instant.parse("2026-07-27T10:00:00Z"), Instant.parse("2026-07-27T10:00:00Z"))),
                null));

        mockMvc.perform(get("/api/tour-operators/{id}/pages", OP)
                        .header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].context").value("pages"))
                .andDo(document("pages/list",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        responseFields(
                                fieldWithPath("data[].id").description("The page id"),
                                fieldWithPath("data[].context").description("The entity's collection: \"pages\""),
                                fieldWithPath("data[].title").description("Display title"),
                                fieldWithPath("data[].handle").description("URL segment (/pages/{handle}), unique per operator"),
                                fieldWithPath("data[].status").description("DRAFT or PUBLISHED"),
                                fieldWithPath("data[].createdAt").description("When created"),
                                fieldWithPath("data[].updatedAt").description("Last content change"),
                                fieldWithPath("nextCursor").description("Opaque cursor; null on the last page").optional())));
    }

    @Test
    void getOne() throws Exception {
        authenticated();
        when(getPageUseCase.execute(eq(UUID.fromString(OP)), eq(UUID.fromString(PAGE)), any()))
                .thenReturn(page());

        mockMvc.perform(get("/api/tour-operators/{id}/pages/{pageId}", OP, PAGE)
                        .header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.context").value("pages"))
                .andExpect(jsonPath("$.body").value("<p>Hello</p>"))
                .andDo(document("pages/get",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        responseFields(
                                fieldWithPath("id").description("The page id"),
                                fieldWithPath("context").description("\"pages\""),
                                fieldWithPath("title").description("Display title"),
                                fieldWithPath("handle").description("URL segment, unique per operator"),
                                fieldWithPath("body").description("Operator-authored raw HTML (stored verbatim; escaping is the renderer's concern)"),
                                fieldWithPath("seoTitle").description("SEO <title> override (≤70); null = derive from the title").optional(),
                                fieldWithPath("seoDescription").description("SEO meta description override (≤320); null = none").optional(),
                                fieldWithPath("status").description("DRAFT or PUBLISHED"),
                                fieldWithPath("templateSuffix").description("Alternate page template (templates/page.{suffix}.json when themes land); null = base template").optional(),
                                fieldWithPath("createdAt").description("When created"),
                                fieldWithPath("updatedAt").description("Last content change"))));
    }

    @Test
    void create() throws Exception {
        authenticated();
        when(createPageUseCase.execute(any())).thenReturn(UUID.fromString(PAGE));

        mockMvc.perform(post("/api/tour-operators/{id}/pages", OP).with(csrf())
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andDo(document("pages/create",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        requestFields(
                                fieldWithPath("title").description("Display title (1–255)"),
                                fieldWithPath("handle").description("Operator-chosen URL segment (handle-shaped, unique per operator — a collision is a 409, never auto-suffixed)"),
                                fieldWithPath("body").description("Raw HTML content (≤256 KiB)"),
                                fieldWithPath("seoTitle").description("Optional SEO title override (≤70)").optional(),
                                fieldWithPath("seoDescription").description("Optional SEO meta description (≤320)").optional())));
    }

    @Test
    void createDuplicateHandleIs409() throws Exception {
        authenticated();
        when(createPageUseCase.execute(any()))
                .thenThrow(new ResourceAlreadyExistsException("A page with this handle already exists"));

        mockMvc.perform(post("/api/tour-operators/{id}/pages", OP).with(csrf())
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isConflict());
    }

    @Test
    void update() throws Exception {
        authenticated();

        mockMvc.perform(patch("/api/tour-operators/{id}/pages/{pageId}", OP, PAGE).with(csrf())
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"About\",\"body\":\"<p>New</p>\",\"templateSuffix\":\"landing\"}"))
                .andExpect(status().isNoContent())
                .andDo(document("pages/update",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        requestFields(
                                fieldWithPath("title").description("Display title (whole replace)"),
                                fieldWithPath("body").description("Raw HTML content (whole replace)"),
                                fieldWithPath("seoTitle").type("String").description("SEO title; null/blank clears").optional(),
                                fieldWithPath("seoDescription").type("String").description("SEO description; null/blank clears").optional(),
                                fieldWithPath("templateSuffix").description("Alternate template suffix; null/blank clears").optional())));
    }

    @Test
    void publishAndUnpublish() throws Exception {
        authenticated();

        mockMvc.perform(post("/api/tour-operators/{id}/pages/{pageId}/publish", OP, PAGE).with(csrf())
                        .header("Authorization", BEARER))
                .andExpect(status().isNoContent())
                .andDo(document("pages/publish",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token"))));

        mockMvc.perform(post("/api/tour-operators/{id}/pages/{pageId}/unpublish", OP, PAGE).with(csrf())
                        .header("Authorization", BEARER))
                .andExpect(status().isNoContent());
    }

    @Test
    void rename() throws Exception {
        authenticated();

        mockMvc.perform(post("/api/tour-operators/{id}/pages/{pageId}/rename", OP, PAGE).with(csrf())
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"handle\":\"about\"}"))
                .andExpect(status().isNoContent())
                .andDo(document("pages/rename",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        requestFields(fieldWithPath("handle")
                                .description("The new canonical handle (handle-shaped; taken → 409)"))));
    }

    @Test
    void deleteOne() throws Exception {
        authenticated();

        mockMvc.perform(delete("/api/tour-operators/{id}/pages/{pageId}", OP, PAGE).with(csrf())
                        .header("Authorization", BEARER))
                .andExpect(status().isNoContent())
                .andDo(document("pages/delete",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token"))));
    }

    @Test
    void getUnknownIs404() throws Exception {
        authenticated();
        when(getPageUseCase.execute(any(), any(), any()))
                .thenThrow(new ResourceNotFoundException("Page not found"));

        mockMvc.perform(get("/api/tour-operators/{id}/pages/{pageId}", OP, PAGE)
                        .header("Authorization", BEARER))
                .andExpect(status().isNotFound());
    }
}

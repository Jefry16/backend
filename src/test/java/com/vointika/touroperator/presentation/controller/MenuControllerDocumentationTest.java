package com.vointika.touroperator.presentation.controller;

import com.vointika.shared.web.docs.ApiErrorSnippets;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.exception.ResourceAlreadyExistsException;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.port.AccessTokenValidatorPort;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.valueobject.Handle;
import com.vointika.shared.web.list.ListQueryParser;
import com.vointika.shared.web.security.SecurityConfig;
import com.vointika.touroperator.application.dto.output.MenuDetail;
import com.vointika.touroperator.application.dto.output.MenuItemNode;
import com.vointika.touroperator.application.usecase.CreateMenuUseCase;
import com.vointika.touroperator.application.usecase.DeleteMenuUseCase;
import com.vointika.touroperator.application.usecase.GetMenuUseCase;
import com.vointika.touroperator.application.usecase.ListMenusUseCase;
import com.vointika.touroperator.application.usecase.RenameMenuUseCase;
import com.vointika.touroperator.application.usecase.ReplaceMenuItemsUseCase;
import com.vointika.touroperator.domain.entity.Menu;
import com.vointika.touroperator.domain.entity.MenuItem;
import com.vointika.touroperator.domain.enums.MenuItemLinkType;
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
import java.util.Map;
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
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MenuController.class)
@ExtendWith(RestDocumentationExtension.class)
@Import({SecurityConfig.class, WebConfig.class, ListQueryParser.class})
class MenuControllerDocumentationTest {

    private static final String OP = "019f7f33-1833-7dc1-b008-47e6c68b3ea2";
    private static final String MENU = "aaaaaaaa-0000-4000-8000-000000000001";
    private static final String USER = "550e8400-e29b-41d4-a716-446655440000";
    private static final String PAGE = "cccccccc-0000-4000-8000-000000000001";
    private static final String TOKEN = "test-access-token";
    private static final String BEARER = "Bearer " + TOKEN;

    private MockMvc mockMvc;

    @MockitoBean private CreateMenuUseCase createUseCase;
    @MockitoBean private RenameMenuUseCase renameUseCase;
    @MockitoBean private GetMenuUseCase getUseCase;
    @MockitoBean private ListMenusUseCase listUseCase;
    @MockitoBean private DeleteMenuUseCase deleteUseCase;
    @MockitoBean private ReplaceMenuItemsUseCase replaceItemsUseCase;
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

    private Menu menu() {
        return new Menu(UUID.fromString(MENU), UUID.fromString(OP), new Handle("main-menu"),
                "Main menu", UUID.fromString(USER),
                Instant.parse("2026-07-28T10:00:00Z"), Instant.parse("2026-07-28T10:00:00Z"));
    }

    private MenuDetail detail() {
        MenuItemNode about = new MenuItemNode(UUID.randomUUID(), "About us",
                MenuItemLinkType.PAGE, UUID.fromString(PAGE), null, Map.of(), List.of());
        MenuItemNode explore = new MenuItemNode(UUID.randomUUID(), "Explore",
                MenuItemLinkType.EXPERIENCE_LIST, null, null,
                Map.of("es", "Explora"), List.of(about));
        return new MenuDetail(menu(), List.of(explore));
    }

    @Test
    void create() throws Exception {
        authenticated();
        when(createUseCase.execute(any())).thenReturn(UUID.fromString(MENU));

        mockMvc.perform(post("/api/tour-operators/{id}/menus", OP)
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"handle\":\"legal\",\"title\":\"Legal\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andDo(document("menus/create",
                        pathParameters(parameterWithName("id").description("The tour operator id")),
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        requestFields(
                                fieldWithPath("handle").description("Handle-shaped theme-facing identifier, unique per operator, immutable — duplicate → 409"),
                                fieldWithPath("title").description("Display title (1–120)"))));
    }

    @Test
    void duplicateHandleIs409() throws Exception {
        authenticated();
        Mockito.doThrow(new ResourceAlreadyExistsException("A menu with this handle already exists"))
                .when(createUseCase).execute(any());

        mockMvc.perform(post("/api/tour-operators/{id}/menus", OP)
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"handle\":\"main-menu\",\"title\":\"Again\"}"))
                .andExpect(status().isConflict())
                .andDo(document("menus/create-conflict",
                        pathParameters(parameterWithName("id").description("The tour operator id")),
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        requestFields(
                                fieldWithPath("handle").description(
                                        "A handle this operator already uses. NOTE: creating an operator "
                                                + "seeds main-menu and footer, so both collide before anyone "
                                                + "has made a menu"),
                                fieldWithPath("title").description("Ignored — the handle decides")),
                        responseFields(ApiErrorSnippets.errorFields())));
    }

    @Test
    void list() throws Exception {
        authenticated();
        when(listUseCase.execute(any(), any())).thenReturn(new CursorPage<>(List.of(menu()), null));

        mockMvc.perform(get("/api/tour-operators/{id}/menus", OP)
                        .header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].context").value("menus"))
                .andDo(document("menus/list",
                        pathParameters(parameterWithName("id").description("The tour operator id")),
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        responseFields(
                                fieldWithPath("data[].id").description("The menu id"),
                                fieldWithPath("data[].context").description("\"menus\""),
                                fieldWithPath("data[].handle").description("The theme-facing identifier"),
                                fieldWithPath("data[].title").description("Display title"),
                                fieldWithPath("data[].createdAt").description("When created"),
                                fieldWithPath("nextCursor").type(JsonFieldType.STRING).description("Opaque cursor; null on the last page").optional())));
    }

    @Test
    void getOne() throws Exception {
        authenticated();
        when(getUseCase.execute(eq(UUID.fromString(OP)), eq(UUID.fromString(MENU)), any()))
                .thenReturn(detail());

        mockMvc.perform(get("/api/tour-operators/{id}/menus/{menuId}", OP, MENU)
                        .header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].children[0].resourceId").value(PAGE))
                .andDo(document("menus/get",
                        pathParameters(parameterWithName("id").description("The tour operator id"),
                                parameterWithName("menuId").description("The menu id")),
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        responseFields(
                                fieldWithPath("id").description("The menu id"),
                                fieldWithPath("context").description("\"menus\""),
                                fieldWithPath("handle").description("The theme-facing identifier"),
                                fieldWithPath("title").description("Display title"),
                                subsectionWithPath("items").description("The item tree: each node has id, title, linkType (HOME | EXPERIENCE_LIST | EXPERIENCE | PAGE | EXTERNAL_URL), resourceId/url per type, titleTranslations (locale → title) and children, position-ordered, max "
                                        + MenuItem.MAX_DEPTH + " levels"),
                                fieldWithPath("createdAt").description("When created"),
                                fieldWithPath("updatedAt").description("When last changed"))));
    }

    @Test
    void rename() throws Exception {
        authenticated();

        mockMvc.perform(patch("/api/tour-operators/{id}/menus/{menuId}", OP, MENU)
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Primary navigation\"}"))
                .andExpect(status().isNoContent())
                .andDo(document("menus/rename",
                        pathParameters(parameterWithName("id").description("The tour operator id"),
                                parameterWithName("menuId").description("The menu id")),
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        requestFields(
                                fieldWithPath("title").description("New display title (1–120); the handle never changes"))));
    }

    @Test
    void replaceItems() throws Exception {
        authenticated();

        mockMvc.perform(put("/api/tour-operators/{id}/menus/{menuId}/items", OP, MENU)
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"title\":\"Home\",\"linkType\":\"HOME\","
                                + "\"titleTranslations\":{\"es\":\"Inicio\"}},"
                                + "{\"title\":\"About us\",\"linkType\":\"PAGE\","
                                + "\"resourceId\":\"" + PAGE + "\"}]}"))
                .andExpect(status().isNoContent())
                .andDo(document("menus/replace-items",
                        pathParameters(parameterWithName("id").description("The tour operator id"),
                                parameterWithName("menuId").description("The menu id")),
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        requestFields(
                                subsectionWithPath("items").description("The WHOLE tree, wholesale (array order = position, "
                                        + "nesting = depth, max " + MenuItem.MAX_DEPTH + " levels). Node: title (1–120), linkType (HOME | EXPERIENCE_LIST | EXPERIENCE | PAGE | EXTERNAL_URL), resourceId (EXPERIENCE/PAGE — must be the operator's, else 422), url (EXTERNAL_URL), titleTranslations (locale → title, supported locales only), children"))));
    }

    @Test
    void replaceItemsRejectsBadTree() throws Exception {
        authenticated();
        Mockito.doThrow(new InvalidFieldException(MenuItem.tooDeepMessage()))
                .when(replaceItemsUseCase).execute(any());

        // A tree four deep, so the published example actually produces the error
        // it documents rather than an empty array that would have succeeded.
        mockMvc.perform(put("/api/tour-operators/{id}/menus/{menuId}/items", OP, MENU)
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"title\":\"One\",\"linkType\":\"HOME\",\"children\":["
                                + "{\"title\":\"Two\",\"linkType\":\"HOME\",\"children\":["
                                + "{\"title\":\"Three\",\"linkType\":\"HOME\",\"children\":["
                                + "{\"title\":\"Four\",\"linkType\":\"HOME\"}]}]}]}]}"))
                .andExpect(status().isUnprocessableEntity())
                .andDo(document("menus/replace-items-invalid",
                        pathParameters(parameterWithName("id").description("The tour operator id"),
                                parameterWithName("menuId").description("The menu id")),
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        responseFields(ApiErrorSnippets.errorFields())));
    }

    @Test
    void deleteOne() throws Exception {
        authenticated();

        mockMvc.perform(delete("/api/tour-operators/{id}/menus/{menuId}", OP, MENU)
                        .header("Authorization", BEARER))
                .andExpect(status().isNoContent())
                .andDo(document("menus/delete",
                        pathParameters(parameterWithName("id").description("The tour operator id"),
                                parameterWithName("menuId").description("The menu id")),
                        requestHeaders(headerWithName("Authorization").description("Bearer access token"))));
    }
}

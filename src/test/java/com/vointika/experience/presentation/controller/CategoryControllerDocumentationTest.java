package com.vointika.experience.presentation.controller;

import com.vointika.experience.application.usecase.CreateCategoryUseCase;
import com.vointika.experience.application.usecase.DeleteCategoryUseCase;
import com.vointika.experience.application.usecase.GetCategoryUseCase;
import com.vointika.experience.application.usecase.ListCategoriesUseCase;
import com.vointika.experience.application.usecase.UpdateCategoryUseCase;
import com.vointika.experience.domain.entity.Category;
import com.vointika.experience.domain.repository.CategoryRepository;
import com.vointika.experience.domain.valueobject.CategoryName;
import com.vointika.shared.exception.ForbiddenException;
import com.vointika.shared.exception.ResourceAlreadyExistsException;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.port.AccessTokenValidatorPort;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.web.docs.ApiErrorSnippets;
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
import org.springframework.restdocs.payload.JsonFieldType;
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
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
@ExtendWith(RestDocumentationExtension.class)
@Import({SecurityConfig.class, WebConfig.class, ListQueryParser.class})
class CategoryControllerDocumentationTest {

    private static final String OP = "019f7f33-1833-7dc1-b008-47e6c68b3ea2";
    private static final String CAT = "cccccccc-0000-4000-8000-000000000001";
    private static final String USER = "550e8400-e29b-41d4-a716-446655440000";
    private static final String TOKEN = "test-access-token";
    private static final String BEARER = "Bearer " + TOKEN;

    /**
     * A STAFF member's token. A 403 turns on <em>who</em> is asking rather than what
     * is asked for, so the request URL is necessarily the one that succeeds for an
     * ADMIN — varying the token is what makes the published example legible.
     * {@code PublishedExamplesAreHonestTest} fails the build without it.
     */
    private static final String STAFF_TOKEN = "staff-access-token";
    private static final String STAFF_BEARER = "Bearer " + STAFF_TOKEN;
    private static final String STAFF_USER = "550e8400-e29b-41d4-a716-4466554400ff";

    private static final String BODY = "{\"name\":\"Boat tours\"}";

    private MockMvc mockMvc;

    @MockitoBean private CreateCategoryUseCase createCategoryUseCase;
    @MockitoBean private UpdateCategoryUseCase updateCategoryUseCase;
    @MockitoBean private ListCategoriesUseCase listCategoriesUseCase;
    @MockitoBean private GetCategoryUseCase getCategoryUseCase;
    @MockitoBean private DeleteCategoryUseCase deleteCategoryUseCase;
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

    private void authenticatedAsStaff() {
        when(accessTokenValidator.isValid(STAFF_TOKEN)).thenReturn(true);
        when(accessTokenValidator.extractUserId(STAFF_TOKEN)).thenReturn(STAFF_USER);
    }

    private Category category() {
        return new Category(UUID.fromString(CAT), UUID.fromString(OP),
                new CategoryName("Boat tours"), Instant.parse("2026-08-21T10:00:00Z"));
    }

    @Test
    void create() throws Exception {
        authenticated();
        when(createCategoryUseCase.execute(any(), any(), any())).thenReturn(UUID.fromString(CAT));

        mockMvc.perform(post("/api/tour-operators/{id}/categories", OP)
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andDo(document("categories/create",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(parameterWithName("id").description("The tour operator id")),
                        requestFields(
                                fieldWithPath("name").description(
                                        "Category name (1–80 characters, unique per operator, compared case-insensitively)"))));
    }

    @Test
    void list() throws Exception {
        authenticated();
        when(listCategoriesUseCase.execute(any(), any()))
                .thenReturn(new CursorPage<>(List.of(category()), null));

        mockMvc.perform(get("/api/tour-operators/{id}/categories", OP)
                        .header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].context").value("categories"))
                .andDo(document("categories/list",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(parameterWithName("id").description("The tour operator id")),
                        responseFields(
                                fieldWithPath("data[].id").description("The category id"),
                                fieldWithPath("data[].context").description("The entity's collection: \"categories\""),
                                fieldWithPath("data[].name").description("Category name"),
                                fieldWithPath("data[].createdAt").description("When created"),
                                fieldWithPath("nextCursor").type(JsonFieldType.STRING)
                                        .description("Opaque cursor; null on the last page").optional())));
    }

    @Test
    void getOne() throws Exception {
        authenticated();
        when(getCategoryUseCase.execute(any(), any(), any())).thenReturn(category());

        mockMvc.perform(get("/api/tour-operators/{id}/categories/{categoryId}", OP, CAT)
                        .header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.context").value("categories"))
                .andDo(document("categories/get",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(
                                parameterWithName("id").description("The tour operator id"),
                                parameterWithName("categoryId").description("The category id")),
                        responseFields(
                                fieldWithPath("id").description("The category id"),
                                fieldWithPath("context").description("The entity's collection: \"categories\""),
                                fieldWithPath("name").description("Category name"),
                                fieldWithPath("createdAt").description("When created"))));
    }

    @Test
    void update() throws Exception {
        authenticated();

        mockMvc.perform(patch("/api/tour-operators/{id}/categories/{categoryId}", OP, CAT)
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Boat trips\"}"))
                .andExpect(status().isNoContent())
                .andDo(document("categories/update",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(
                                parameterWithName("id").description("The tour operator id"),
                                parameterWithName("categoryId").description("The category id")),
                        // `.optional()` publishes nothing — the default table has no
                        // such column — so the omission rule goes in the text.
                        requestFields(
                                fieldWithPath("name").description(
                                        "Category name (1–80, unique per operator, case-insensitively). "
                                        + "Omit to keep the current value; the name is the only editable field, "
                                        + "so omitting it makes the whole call a no-op").optional())));
    }

    @Test
    void deleteOne() throws Exception {
        authenticated();

        mockMvc.perform(delete("/api/tour-operators/{id}/categories/{categoryId}", OP, CAT)
                        .header("Authorization", BEARER))
                .andExpect(status().isNoContent())
                .andDo(document("categories/delete",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(
                                parameterWithName("id").description("The tour operator id"),
                                parameterWithName("categoryId").description("The category id"))));
    }

    @Test
    void createRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/tour-operators/{id}/categories", OP)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void staffCannotCreate() throws Exception {
        authenticatedAsStaff();
        doThrow(new ForbiddenException(TourOperatorMembershipCheck.requiresRoleMessage("ADMIN")))
                .when(createCategoryUseCase).execute(any(), any(), any());

        mockMvc.perform(post("/api/tour-operators/{id}/categories", OP)
                        .header("Authorization", STAFF_BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden())
                .andDo(document("categories/create-forbidden",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(parameterWithName("id").description("The tour operator id")),
                        responseFields(ApiErrorSnippets.errorFields())));
    }

    @Test
    void duplicateNameIsConflict() throws Exception {
        authenticated();
        // Send the name that clashes, not the one that succeeds two sections above.
        doThrow(new ResourceAlreadyExistsException(CategoryRepository.NAME_TAKEN))
                .when(createCategoryUseCase).execute(any(), any(), any());

        mockMvc.perform(post("/api/tour-operators/{id}/categories", OP)
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"boat tours\"}"))
                .andExpect(status().isConflict())
                .andDo(document("categories/create-conflict",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(parameterWithName("id").description("The tour operator id")),
                        responseFields(ApiErrorSnippets.errorFields())));
    }

    /**
     * The stub is {@code ensureMember}, so this publishes the <b>tenant</b> 404 —
     * identical to the answer for an operator that does not exist. Reaching for
     * {@code CategoryRepository.NOT_FOUND} here would publish "Category not found"
     * for a non-member and confirm the operator resolved (PATTERNS §9a).
     */
    @Test
    void nonMemberGets404() throws Exception {
        authenticatedAsStaff();
        doThrow(new ResourceNotFoundException(TourOperatorMembershipCheck.TENANT_NOT_FOUND))
                .when(membershipCheck).ensureMember(eq(UUID.fromString(STAFF_USER)), eq(UUID.fromString(OP)));

        mockMvc.perform(get("/api/tour-operators/{id}/categories", OP)
                        .header("Authorization", STAFF_BEARER))
                .andExpect(status().isNotFound())
                .andDo(document("categories/list-not-found",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(parameterWithName("id").description("The tour operator id")),
                        responseFields(ApiErrorSnippets.errorFields())));
    }

    /** This one really is the category 404 — the id in the URL is the thing missing. */
    @Test
    void anUnknownCategoryIs404() throws Exception {
        authenticated();
        doThrow(new ResourceNotFoundException(CategoryRepository.NOT_FOUND))
                .when(getCategoryUseCase).execute(any(), any(), any());

        mockMvc.perform(get("/api/tour-operators/{id}/categories/{categoryId}", OP,
                        "cccccccc-0000-4000-8000-0000000000ff")
                        .header("Authorization", BEARER))
                .andExpect(status().isNotFound())
                .andDo(document("categories/get-not-found",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(
                                parameterWithName("id").description("The tour operator id"),
                                parameterWithName("categoryId").description("The category id")),
                        responseFields(ApiErrorSnippets.errorFields())));
    }
}

package com.vointika.audience.presentation.controller;

import com.vointika.audience.domain.repository.AudienceRepository;
import com.vointika.audience.application.usecase.CreateAudienceUseCase;
import com.vointika.audience.application.usecase.GetAudienceUseCase;
import com.vointika.audience.application.usecase.ListAudiencesUseCase;
import com.vointika.audience.application.usecase.UpdateAudienceUseCase;
import com.vointika.audience.domain.entity.Audience;
import com.vointika.audience.domain.valueobject.AudienceName;
import com.vointika.audience.domain.valueobject.PaxPerUnit;
import com.vointika.shared.exception.ForbiddenException;
import com.vointika.shared.exception.ResourceAlreadyExistsException;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.web.docs.ApiErrorSnippets;
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

@WebMvcTest(AudienceController.class)
@ExtendWith(RestDocumentationExtension.class)
@Import({SecurityConfig.class, WebConfig.class, ListQueryParser.class})
class AudienceControllerDocumentationTest {

    private static final String OP = "019f7f33-1833-7dc1-b008-47e6c68b3ea2";
    private static final String AUD = "aaaaaaaa-0000-4000-8000-000000000001";
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
    private static final String BODY = "{\"name\":\"Adults\",\"paxPerUnit\":1}";

    private MockMvc mockMvc;

    @MockitoBean private CreateAudienceUseCase createAudienceUseCase;
    @MockitoBean private UpdateAudienceUseCase updateAudienceUseCase;
    @MockitoBean private ListAudiencesUseCase listAudiencesUseCase;
    @MockitoBean private GetAudienceUseCase getAudienceUseCase;
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

    private Audience audience() {
        return new Audience(UUID.fromString(AUD), UUID.fromString(OP),
                new AudienceName("Adults"), new PaxPerUnit(1),
                UUID.fromString(USER), Instant.parse("2026-07-25T10:00:00Z"));
    }

    @Test
    void create() throws Exception {
        authenticated();
        when(createAudienceUseCase.execute(any(), any(), any())).thenReturn(UUID.fromString(AUD));

        mockMvc.perform(post("/api/tour-operators/{id}/audiences", OP)
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andDo(document("audiences/create",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(parameterWithName("id").description("The tour operator id")),
                        requestFields(
                                fieldWithPath("name").description("Tier name (1–80, unique per operator)"),
                                fieldWithPath("paxPerUnit").description("People per unit (positive; defaults to 1)").optional())));
    }

    @Test
    void list() throws Exception {
        authenticated();
        when(listAudiencesUseCase.execute(any(), any()))
                .thenReturn(new CursorPage<>(List.of(audience()), null));

        mockMvc.perform(get("/api/tour-operators/{id}/audiences", OP)
                        .header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].context").value("audiences"))
                .andDo(document("audiences/list",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(parameterWithName("id").description("The tour operator id")),
                        responseFields(
                                fieldWithPath("data[].id").description("The audience id"),
                                fieldWithPath("data[].context").description("The entity's collection: \"audiences\""),
                                fieldWithPath("data[].name").description("Tier name"),
                                fieldWithPath("data[].paxPerUnit").description("People per unit"),
                                fieldWithPath("data[].createdAt").description("When created"),
                                fieldWithPath("nextCursor").type(JsonFieldType.STRING).description("Opaque cursor; null on the last page").optional())));
    }

    @Test
    void getOne() throws Exception {
        authenticated();
        when(getAudienceUseCase.execute(any(), any(), any())).thenReturn(audience());

        mockMvc.perform(get("/api/tour-operators/{id}/audiences/{audienceId}", OP, AUD)
                        .header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.context").value("audiences"))
                .andDo(document("audiences/get",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(parameterWithName("id").description("The tour operator id"), parameterWithName("audienceId").description("The audience id")),
                        responseFields(
                                fieldWithPath("id").description("The audience id"),
                                fieldWithPath("context").description("The entity's collection: \"audiences\""),
                                fieldWithPath("name").description("Tier name"),
                                fieldWithPath("paxPerUnit").description("People per unit"),
                                fieldWithPath("createdAt").description("When created"))));
    }

    @Test
    void update() throws Exception {
        authenticated();

        mockMvc.perform(patch("/api/tour-operators/{id}/audiences/{audienceId}", OP, AUD)
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isNoContent())
                .andDo(document("audiences/update",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(
                                parameterWithName("id").description("The tour operator id"),
                                parameterWithName("audienceId").description("The audience id")),
                        // PATCH is partial: UpdateAudienceUseCase skips a null field, so an
                        // omitted paxPerUnit keeps its current value rather than resetting to
                        // 1. `.optional()` publishes nothing — the table has no such column —
                        // so both rules go in the text.
                        requestFields(
                                fieldWithPath("name").description("Tier name (1–80, unique per operator). Omit to keep the current value").optional(),
                                fieldWithPath("paxPerUnit").description("People per unit (positive). Omit to keep the current value — it is not reset to 1").optional())));
    }


    @Test
    void createRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/tour-operators/{id}/audiences", OP)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void staffCannotCreate() throws Exception {
        authenticatedAsStaff();
        doThrow(new ForbiddenException(TourOperatorMembershipCheck.requiresRoleMessage("ADMIN"))).when(createAudienceUseCase).execute(any(), any(), any());
        mockMvc.perform(post("/api/tour-operators/{id}/audiences", OP)
                        .header("Authorization", STAFF_BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden())
                .andDo(document("audiences/create-forbidden",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(parameterWithName("id").description("The tour operator id")),
                        responseFields(ApiErrorSnippets.errorFields())));
    }

    @Test
    void duplicateNameIsConflict() throws Exception {
        authenticated();
        // Send the name that clashes, not the one that succeeds two sections above.
        doThrow(new ResourceAlreadyExistsException(AudienceRepository.NAME_TAKEN))
                .when(createAudienceUseCase).execute(any(), any(), any());
        mockMvc.perform(post("/api/tour-operators/{id}/audiences", OP)
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"adult\",\"paxPerUnit\":1}"))
                .andExpect(status().isConflict())
                .andDo(document("audiences/create-conflict",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(parameterWithName("id").description("The tour operator id")),
                        responseFields(ApiErrorSnippets.errorFields())));
    }

    @Test
    void nonMemberGets404() throws Exception {
        authenticatedAsStaff();
        // ensureMember raises the TENANT 404, not the audience one — the point of this
        // endpoint's 404 is that it says nothing about whether the operator exists.
        doThrow(new ResourceNotFoundException(TourOperatorMembershipCheck.TENANT_NOT_FOUND))
                .when(membershipCheck).ensureMember(eq(UUID.fromString(STAFF_USER)), eq(UUID.fromString(OP)));
        mockMvc.perform(get("/api/tour-operators/{id}/audiences", OP)
                        .header("Authorization", STAFF_BEARER))
                .andExpect(status().isNotFound())
                .andDo(document("audiences/list-not-found",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(parameterWithName("id").description("The tour operator id")),
                        responseFields(ApiErrorSnippets.errorFields())));
    }
}

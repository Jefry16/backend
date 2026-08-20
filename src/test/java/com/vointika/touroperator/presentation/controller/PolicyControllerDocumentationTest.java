package com.vointika.touroperator.presentation.controller;

import com.vointika.shared.exception.ForbiddenException;
import com.vointika.shared.web.docs.ApiErrorSnippets;
import com.vointika.shared.exception.ResourceAlreadyExistsException;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.AccessTokenValidatorPort;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.web.security.SecurityConfig;
import com.vointika.touroperator.application.dto.output.PolicyView;
import com.vointika.touroperator.application.usecase.DeletePolicyUseCase;
import com.vointika.touroperator.application.usecase.GetPolicyUseCase;
import com.vointika.touroperator.application.usecase.ListPoliciesUseCase;
import com.vointika.touroperator.application.usecase.CreatePolicyUseCase;
import com.vointika.touroperator.application.usecase.UpdatePolicyUseCase;
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

import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.FilterSpec;
import com.vointika.shared.list.ListQuery;
import com.vointika.shared.list.SortDirection;
import com.vointika.shared.list.SortSpec;
import com.vointika.shared.web.list.ListQueryParser;

import java.time.Instant;
import java.util.UUID;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
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
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PolicyController.class)
@ExtendWith(RestDocumentationExtension.class)
@Import({SecurityConfig.class, WebConfig.class})
class PolicyControllerDocumentationTest {

    private static final String OPERATOR_ID = "019f7f33-1833-7dc1-b008-47e6c68b3ea2";
    private static final String USER_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final String STAFF_USER_ID = "7c9e6679-7425-40de-944b-e07fc1f90ae7";
    private static final String POLICY_ID = "019f8000-0000-7000-8000-000000000001";
    private static final String MISSING_POLICY = "019f8000-0000-7000-8000-0000000000ff";
    private static final String CREATE_BODY =
            "{\"type\":\"CANCELLATION\",\"title\":\"Cancellation policy\","
                    + "\"body\":\"<h2>Cancellations</h2><p>Free up to 48h before.</p>\"}";
    private static final String UPDATE_BODY =
            "{\"title\":\"Cancellation policy\","
                    + "\"body\":\"<h2>Cancellations</h2><p>Free up to 48h before.</p>\"}";

    private MockMvc mockMvc;

    @MockitoBean private ListPoliciesUseCase listUseCase;
    @MockitoBean private GetPolicyUseCase getUseCase;
    @MockitoBean private CreatePolicyUseCase createUseCase;
    @MockitoBean private UpdatePolicyUseCase updateUseCase;
    @MockitoBean private DeletePolicyUseCase deleteUseCase;
    @MockitoBean private ListQueryParser listQueryParser;
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
        when(accessTokenValidator.isValid("test-access-token")).thenReturn(true);
        when(accessTokenValidator.extractUserId("test-access-token")).thenReturn(USER_ID);
    }

    private static PolicyView view() {
        return new PolicyView(UUID.fromString(POLICY_ID), "CANCELLATION", "Cancellation policy",
                "<h2>Cancellations</h2><p>Free up to 48h before.</p>",
                Instant.parse("2026-08-01T10:00:00Z"), Instant.parse("2026-08-06T12:00:00Z"));
    }

    @Test
    void list() throws Exception {
        authenticated();
        when(listQueryParser.parse(any(), any(), any())).thenReturn(
                new ListQuery(UUID.fromString(OPERATOR_ID), FilterSpec.empty(),
                        new SortSpec("type", SortDirection.ASC), null));
        when(listUseCase.execute(any(), any()))
                .thenReturn(new CursorPage<>(List.of(view()), null));

        mockMvc.perform(get("/api/tour-operators/{id}/policies", OPERATOR_ID)
                        .header("Authorization", "Bearer test-access-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].type").value("CANCELLATION"))
                .andDo(document("tour-operators/policies/list",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(parameterWithName("id").description("The tour operator id")),
                        responseFields(
                                fieldWithPath("data[].id").description("The policy id"),
                                fieldWithPath("data[].context").description("The entity's collection: \"policies\""),
                                fieldWithPath("data[].type").description(
                                        "CANCELLATION, PRIVACY, TERMS or LEGAL_NOTICE"),
                                fieldWithPath("data[].title")
                                        .description("The document's heading, and its title tag"),
                                fieldWithPath("data[].body")
                                        .description("Raw HTML, stored and returned verbatim"),
                                fieldWithPath("data[].createdAt").description("When the policy was first written"),
                                fieldWithPath("data[].updatedAt").description("When its text last changed"),
                                fieldWithPath("nextCursor").type(JsonFieldType.STRING).description(
                                        "Opaque keyset cursor, null on the last page. Four policies never "
                                                + "paginate, but the grammar is every tenant list's")
                                        .optional())));
    }

    @Test
    void getOne() throws Exception {
        authenticated();
        when(getUseCase.execute(any(), any(), any())).thenReturn(view());

        mockMvc.perform(get("/api/tour-operators/{id}/policies/{policyId}", OPERATOR_ID, POLICY_ID)
                        .header("Authorization", "Bearer test-access-token"))
                .andExpect(status().isOk())
                .andDo(document("tour-operators/policies/get",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(
                                parameterWithName("id").description("The tour operator id"),
                                parameterWithName("policyId").description(
                                        "The policy id; one belonging to another operator is a 404")),
                        responseFields(
                                fieldWithPath("id").description("The policy id"),
                                fieldWithPath("context").description("The entity's collection: \"policies\""),
                                fieldWithPath("type").description("The policy type"),
                                fieldWithPath("title").description("The document's heading, and its title tag"),
                                fieldWithPath("body").description("Raw HTML, stored and returned verbatim"),
                                fieldWithPath("createdAt").description("When the policy was first written"),
                                fieldWithPath("updatedAt").description("When its text last changed"))));
    }

    @Test
    void create() throws Exception {
        authenticated();
        when(createUseCase.execute(any(), any(), any())).thenReturn(UUID.fromString(POLICY_ID));

        mockMvc.perform(post("/api/tour-operators/{id}/policies", OPERATOR_ID)
                        .header("Authorization", "Bearer test-access-token")
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location",
                        "/api/tour-operators/" + OPERATOR_ID + "/policies/" + POLICY_ID))
                .andDo(document("tour-operators/policies/create",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(parameterWithName("id").description("The tour operator id")),
                        requestFields(
                                fieldWithPath("type").description(
                                        "CANCELLATION, PRIVACY, TERMS or LEGAL_NOTICE. Immutable "
                                                + "afterwards; a repeat is a 409"),
                                fieldWithPath("title").description("Required, ≤200 characters"),
                                fieldWithPath("body").description(
                                        "Required. Raw HTML, stored verbatim and rendered unescaped on the "
                                                + "storefront; ≤262144 characters"))));
    }

    @Test
    void aSecondPolicyOfTheSameTypeIsAConflict() throws Exception {
        authenticated();
        doThrow(new ResourceAlreadyExistsException("A CANCELLATION policy already exists"))
                .when(createUseCase).execute(any(), any(), any());

        mockMvc.perform(post("/api/tour-operators/{id}/policies", OPERATOR_ID)
                        .header("Authorization", "Bearer test-access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"CANCELLATION\",\"title\":\"Cancellations (v2)\","
                                + "\"body\":\"<p>Superseded copy.</p>\"}"))
                .andExpect(status().isConflict())
                .andDo(document("tour-operators/policies/create-conflict",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(parameterWithName("id").description("The tour operator id")),
                        requestFields(
                                fieldWithPath("type").description(
                                        "A type this operator already has. One policy per type, forever — "
                                                + "to replace the text, PUT the existing one"),
                                fieldWithPath("title").description("Ignored — the type decides"),
                                fieldWithPath("body").description("Ignored — the type decides")),
                        responseFields(ApiErrorSnippets.errorFields())));
    }

    @Test
    void update() throws Exception {
        authenticated();

        mockMvc.perform(put("/api/tour-operators/{id}/policies/{policyId}", OPERATOR_ID, POLICY_ID)
                        .header("Authorization", "Bearer test-access-token")
                        .contentType(MediaType.APPLICATION_JSON).content(UPDATE_BODY))
                .andExpect(status().isNoContent())
                .andDo(document("tour-operators/policies/update",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(
                                parameterWithName("id").description("The tour operator id"),
                                parameterWithName("policyId").description("The policy id")),
                        requestFields(
                                fieldWithPath("title").description("Required, ≤200 characters"),
                                fieldWithPath("body").description(
                                        "Required. Raw HTML, stored verbatim; ≤262144 characters. The type "
                                                + "is not settable — it is the storefront address"))));
    }

    @Test
    void deleteOne() throws Exception {
        authenticated();

        mockMvc.perform(delete("/api/tour-operators/{id}/policies/{policyId}", OPERATOR_ID, POLICY_ID)
                        .header("Authorization", "Bearer test-access-token"))
                .andExpect(status().isNoContent())
                .andDo(document("tour-operators/policies/delete",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(
                                parameterWithName("id").description("The tour operator id"),
                                parameterWithName("policyId").description(
                                        "Removes the policy, taking the storefront page with it. "
                                                + "An unknown or foreign id is an idempotent success"))));
    }

    @Test
    void anotherOperatorsPolicyIsNotFound() throws Exception {
        // The lookup binds id to tenant, so a valid id from another operator is
        // byte-identical to one that does not exist.
        authenticated();
        doThrow(new ResourceNotFoundException("Policy not found"))
                .when(getUseCase).execute(any(), any(), any());

        mockMvc.perform(get("/api/tour-operators/{id}/policies/{policyId}", OPERATOR_ID, MISSING_POLICY)
                        .header("Authorization", "Bearer test-access-token"))
                .andExpect(status().isNotFound())
                .andDo(document("tour-operators/policies/get-not-found",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(parameterWithName("id").description("The tour operator id"),
                                parameterWithName("policyId").description(
                                        "A policy id that does not exist, or belongs to another operator — "
                                                + "the two are byte-identical here")),
                        responseFields(ApiErrorSnippets.errorFields())));
    }

    /**
     * <b>The role line, published once for this context.</b> Twenty-one of the
     * forty endpoints here gate on ADMIN+ or OWNER and the guide showed a reader
     * none of them. A STAFF member lists and reads policies and is refused on
     * every write — 403, not 404, because the membership check has already passed.
     */
    @Test
    void aStaffMemberCannotWriteAPolicy() throws Exception {
        when(accessTokenValidator.isValid("staff-access-token")).thenReturn(true);
        when(accessTokenValidator.extractUserId("staff-access-token")).thenReturn(STAFF_USER_ID);
        doThrow(new ForbiddenException(TourOperatorMembershipCheck.requiresRoleMessage("ADMIN")))
                .when(createUseCase).execute(any(), any(), any());

        mockMvc.perform(post("/api/tour-operators/{id}/policies", OPERATOR_ID)
                        .header("Authorization", "Bearer staff-access-token")
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isForbidden())
                .andDo(document("tour-operators/policies/create-forbidden",
                        requestHeaders(headerWithName("Authorization").description(
                                "Bearer access token for a STAFF member — this error is about who asks, "
                                        + "so the URL and the body are the successful call's")),
                        pathParameters(parameterWithName("id").description("The tour operator id")),
                        responseFields(ApiErrorSnippets.errorFields())));
    }

    @Test
    void anonymousIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/tour-operators/{id}/policies", OPERATOR_ID))
                .andExpect(status().isUnauthorized());
    }
}

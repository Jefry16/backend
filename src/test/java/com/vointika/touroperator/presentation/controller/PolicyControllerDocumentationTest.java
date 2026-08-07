package com.vointika.touroperator.presentation.controller;

import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.AccessTokenValidatorPort;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.web.security.SecurityConfig;
import com.vointika.touroperator.application.dto.output.PolicyView;
import com.vointika.touroperator.application.usecase.DeletePolicyUseCase;
import com.vointika.touroperator.application.usecase.GetPolicyUseCase;
import com.vointika.touroperator.application.usecase.ListPoliciesUseCase;
import com.vointika.touroperator.application.usecase.UpsertPolicyUseCase;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.http.HttpDocumentation.httpRequest;
import static org.springframework.restdocs.http.HttpDocumentation.httpResponse;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PolicyController.class)
@ExtendWith(RestDocumentationExtension.class)
@Import({SecurityConfig.class, WebConfig.class})
class PolicyControllerDocumentationTest {

    private static final String OPERATOR_ID = "019f7f33-1833-7dc1-b008-47e6c68b3ea2";
    private static final String USER_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final String BODY =
            "{\"title\":\"Cancellation policy\","
                    + "\"body\":\"<h2>Cancellations</h2><p>Free up to 48h before.</p>\"}";

    private MockMvc mockMvc;

    @MockitoBean private ListPoliciesUseCase listUseCase;
    @MockitoBean private GetPolicyUseCase getUseCase;
    @MockitoBean private UpsertPolicyUseCase upsertUseCase;
    @MockitoBean private DeletePolicyUseCase deleteUseCase;
    @MockitoBean private TourOperatorMembershipCheck membershipCheck;
    @MockitoBean private AccessTokenValidatorPort accessTokenValidator;

    @BeforeEach
    void setUp(WebApplicationContext context, RestDocumentationContextProvider restDocumentation) {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(documentationConfiguration(restDocumentation)
                        .operationPreprocessors()
                        .withRequestDefaults(prettyPrint())
                        .withResponseDefaults(prettyPrint())
                        .and()
                        .snippets().withDefaults(httpRequest(), httpResponse()))
                .apply(springSecurity())
                .build();
    }

    private void authenticated() {
        when(accessTokenValidator.isValid("test-access-token")).thenReturn(true);
        when(accessTokenValidator.extractUserId("test-access-token")).thenReturn(USER_ID);
    }

    private static PolicyView view() {
        return new PolicyView("CANCELLATION", "Cancellation policy",
                "<h2>Cancellations</h2><p>Free up to 48h before.</p>",
                Instant.parse("2026-08-01T10:00:00Z"), Instant.parse("2026-08-06T12:00:00Z"));
    }

    @Test
    void list() throws Exception {
        authenticated();
        when(listUseCase.execute(any(), any())).thenReturn(List.of(view()));

        mockMvc.perform(get("/api/tour-operators/{id}/policies", OPERATOR_ID)
                        .header("Authorization", "Bearer test-access-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("CANCELLATION"))
                .andDo(document("tour-operators/policies/list",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(parameterWithName("id").description("The tour operator id")),
                        responseFields(
                                fieldWithPath("[].type").description(
                                        "CANCELLATION, PRIVACY, TERMS or LEGAL_NOTICE"),
                                fieldWithPath("[].title").description("The document's heading, and its title tag"),
                                fieldWithPath("[].body").description("Raw HTML, stored and returned verbatim"),
                                fieldWithPath("[].createdAt").description("When the policy was first written"),
                                fieldWithPath("[].updatedAt").description("When its text last changed"))));
    }

    @Test
    void getOne() throws Exception {
        authenticated();
        when(getUseCase.execute(any(), any(), any())).thenReturn(view());

        mockMvc.perform(get("/api/tour-operators/{id}/policies/{type}", OPERATOR_ID, "CANCELLATION")
                        .header("Authorization", "Bearer test-access-token"))
                .andExpect(status().isOk())
                .andDo(document("tour-operators/policies/get",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(
                                parameterWithName("id").description("The tour operator id"),
                                parameterWithName("type").description(
                                        "The policy type; one the operator has not written is a 404")),
                        responseFields(
                                fieldWithPath("type").description("The policy type"),
                                fieldWithPath("title").description("The document's heading, and its title tag"),
                                fieldWithPath("body").description("Raw HTML, stored and returned verbatim"),
                                fieldWithPath("createdAt").description("When the policy was first written"),
                                fieldWithPath("updatedAt").description("When its text last changed"))));
    }

    @Test
    void upsert() throws Exception {
        authenticated();

        mockMvc.perform(put("/api/tour-operators/{id}/policies/{type}", OPERATOR_ID, "CANCELLATION")
                        .header("Authorization", "Bearer test-access-token")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isNoContent())
                .andDo(document("tour-operators/policies/upsert",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(
                                parameterWithName("id").description("The tour operator id"),
                                parameterWithName("type").description(
                                        "Creates the policy of this type, or replaces its text")),
                        requestFields(
                                fieldWithPath("title").description("Required, ≤200 characters"),
                                fieldWithPath("body").description(
                                        "Required. Raw HTML, stored verbatim and rendered unescaped on the "
                                                + "storefront; ≤262144 characters"))));
    }

    @Test
    void deleteOne() throws Exception {
        authenticated();

        mockMvc.perform(delete("/api/tour-operators/{id}/policies/{type}", OPERATOR_ID, "CANCELLATION")
                        .header("Authorization", "Bearer test-access-token"))
                .andExpect(status().isNoContent())
                .andDo(document("tour-operators/policies/delete",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(
                                parameterWithName("id").description("The tour operator id"),
                                parameterWithName("type").description(
                                        "Removes the policy, taking the storefront page with it. "
                                                + "Deleting an unwritten one is an idempotent success"))));
    }

    @Test
    void anUnknownTypeIsNotFound() throws Exception {
        // The path segment reaches PolicyType.from, never valueOf — so a name no
        // type is called is a 404 and not a 500.
        authenticated();
        doThrow(new ResourceNotFoundException("Policy not found"))
                .when(getUseCase).execute(any(), any(), any());

        mockMvc.perform(get("/api/tour-operators/{id}/policies/{type}", OPERATOR_ID, "REFUNDS")
                        .header("Authorization", "Bearer test-access-token"))
                .andExpect(status().isNotFound());
    }

    @Test
    void anonymousIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/tour-operators/{id}/policies", OPERATOR_ID))
                .andExpect(status().isUnauthorized());
    }
}

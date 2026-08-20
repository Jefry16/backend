package com.vointika.touroperator.presentation.controller;

import com.vointika.shared.web.docs.ApiErrorSnippets;
import com.vointika.shared.exception.ResourceAlreadyExistsException;
import com.vointika.shared.port.AccessTokenValidatorPort;
import com.vointika.shared.web.security.SecurityConfig;
import com.vointika.touroperator.application.dto.output.CreateTourOperatorOutput;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.touroperator.application.dto.output.AddressView;
import com.vointika.touroperator.application.dto.output.TourOperatorView;
import com.vointika.touroperator.application.usecase.GetTourOperatorUseCase;
import com.vointika.touroperator.application.usecase.UpdateTourOperatorUseCase;
import com.vointika.touroperator.application.usecase.CreateTourOperatorUseCase;
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

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.patch;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TourOperatorController.class)
@ExtendWith(RestDocumentationExtension.class)
@Import(SecurityConfig.class)
class TourOperatorControllerDocumentationTest {

    private MockMvc mockMvc;

    @MockitoBean
    private CreateTourOperatorUseCase createTourOperatorUseCase;

    @MockitoBean
    private GetTourOperatorUseCase getUseCase;

    @MockitoBean
    private UpdateTourOperatorUseCase updateUseCase;

    @MockitoBean
    private TourOperatorMembershipCheck membershipCheck;

    @MockitoBean
    private AccessTokenValidatorPort accessTokenValidator;

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

    @Test
    void create() throws Exception {
        UUID newId = UUID.fromString("7f000001-0000-4000-8000-000000000001");
        when(accessTokenValidator.isValid("test-access-token")).thenReturn(true);
        when(accessTokenValidator.extractUserId("test-access-token"))
                .thenReturn("550e8400-e29b-41d4-a716-446655440000");
        when(createTourOperatorUseCase.execute(any()))
                .thenReturn(new CreateTourOperatorOutput(newId));

        mockMvc.perform(post("/api/tour-operators")
                        .header("Authorization", "Bearer test-access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Acme Tours",
                                    "address": {
                                        "address1": "123 Beach Rd",
                                        "address2": "Suite 2",
                                        "city": "Punta Cana",
                                        "province": "La Altagracia",
                                        "zip": "23000",
                                        "countryId": "33333333-3333-3333-3333-333333333333"
                                    },
                                    "timezoneId": "cccccccc-cccc-cccc-cccc-cccccccccccc",
                                    "currencyId": "cccc0001-0000-0000-0000-000000000001"
                                }"""))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/tour-operators/" + newId))
                .andDo(document("tour-operators/create",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        requestFields(
                                fieldWithPath("name").description("The operator's display name (2-150 chars)"),
                                fieldWithPath("address").description("The operator's postal address"),
                                fieldWithPath("address.address1").description("Street line (required, <=255)"),
                                fieldWithPath("address.address2").description("Second line, or omitted").optional(),
                                fieldWithPath("address.city").description("City (required, <=120)"),
                                fieldWithPath("address.province").description("Province or region, or omitted").optional(),
                                fieldWithPath("address.zip").description("Postcode, or omitted — no format is enforced, because there is no universal one").optional(),
                                fieldWithPath("address.countryId").description("A reference country id (GET /api/countries)"),
                                fieldWithPath("timezoneId").description("A reference timezone id (GET /api/timezones)"),
                                fieldWithPath("currencyId").description("A reference currency id (GET /api/currencies)")),
                        responseHeaders(headerWithName("Location").description("URI of the created tour operator"))));
    }

    /**
     * The double-submit guard, and the only 409 on create. It is scoped to the
     * caller: two different owners may both run "Sunset Tours". The handle is not
     * the conflict — a colliding handle is suffixed silently, so "Sunset Tours"
     * becomes {@code sunset-tours-2} rather than being refused.
     */
    @Test
    void aSecondOperatorWithTheSameNameIs409() throws Exception {
        authenticate();
        when(createTourOperatorUseCase.execute(any()))
                .thenThrow(new ResourceAlreadyExistsException(
                        "You already have an operator named \"Acme Tours\""));

        mockMvc.perform(post("/api/tour-operators")
                        .header("Authorization", "Bearer test-access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Acme Tours",
                                    "address": {
                                        "address1": "9 Second Street",
                                        "city": "Punta Cana",
                                        "countryId": "33333333-3333-3333-3333-333333333333"
                                    },
                                    "timezoneId": "cccccccc-cccc-cccc-cccc-cccccccccccc",
                                    "currencyId": "cccc0001-0000-0000-0000-000000000001"
                                }"""))
                .andExpect(status().isConflict())
                .andDo(document("tour-operators/create-conflict",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        responseFields(ApiErrorSnippets.errorFields())));
    }

    @Test
    void createRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/tour-operators")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Acme Tours",
                                    "address": "123 Beach Rd",
                                    "timezoneId": "cccccccc-cccc-cccc-cccc-cccccccccccc",
                                    "currencyId": "cccc0001-0000-0000-0000-000000000001"
                                }"""))
                .andExpect(status().isUnauthorized());
    }

    private static final String OPERATOR_ID = "019f7f33-1833-7dc1-b008-47e6c68b3ea2";

    private void authenticate() {
        when(accessTokenValidator.isValid("test-access-token")).thenReturn(true);
        when(accessTokenValidator.extractUserId("test-access-token"))
                .thenReturn("550e8400-e29b-41d4-a716-446655440000");
    }

    @Test
    void getDetails() throws Exception {
        authenticate();
        when(getUseCase.execute(any(), any())).thenReturn(new TourOperatorView(
                java.util.UUID.fromString(OPERATOR_ID), "Acme Tours", "acme", new AddressView("Calle Mayor 1", null, "Calle Mayor 1", "Madrid", "Madrid", "28013",
                        UUID.fromString("11111111-1111-1111-1111-111111111111"), "ES", "Spain"),
                "+34 600 000 000", "hola@acme.test",
                java.util.UUID.randomUUID(), java.util.UUID.randomUUID(),
                java.time.Instant.parse("2026-01-01T00:00:00Z"),
                java.time.Instant.parse("2026-08-08T00:00:00Z")));

        mockMvc.perform(get("/api/tour-operators/{id}", OPERATOR_ID)
                        .header("Authorization", "Bearer test-access-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value("+34 600 000 000"))
                .andExpect(jsonPath("$.context").value("tour-operators"))
                .andDo(document("tour-operators/get",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(parameterWithName("id").description("The tour operator id")),
                        responseFields(
                                fieldWithPath("id").description("The tour operator id"),
                                fieldWithPath("context").description("The entity's collection: \"tour-operators\""),
                                fieldWithPath("name").description("The business name"),
                                fieldWithPath("handle").description(
                                        "The storefront subdomain — immutable, not editable"),
                                fieldWithPath("address").description(
                                        "The business address, or null for an operator that has not entered one").optional(),
                                fieldWithPath("address.address1").description("Street line").optional(),
                                fieldWithPath("address.address2").type(JsonFieldType.STRING)
                                        .description("Second line, or null").optional(),
                                fieldWithPath("address.city").description("City").optional(),
                                fieldWithPath("address.province").description("Province or region, or null").optional(),
                                fieldWithPath("address.zip").description("Postcode, or null").optional(),
                                fieldWithPath("address.countryId").description("Reference country id").optional(),
                                fieldWithPath("address.countryCode").description("ISO 3166-1 alpha-2 code").optional(),
                                fieldWithPath("address.countryName").description("Country name, in English").optional(),
                                fieldWithPath("phone")
                                        .description("Public contact phone, or null").optional(),
                                fieldWithPath("email")
                                        .description("Public contact email, or null").optional(),
                                fieldWithPath("timezoneId").description("Reference timezone id"),
                                fieldWithPath("currencyId").description("Reference currency id"),
                                fieldWithPath("createdAt").description("When the operator was created"),
                                fieldWithPath("updatedAt").description("When its details last changed"))));
    }

    /**
     * Sends <b>every</b> field {@code UpdateTourOperatorInput} accepts, not the
     * two a realistic edit would. Strict {@code requestFields} fails on an
     * undocumented field that is <em>present</em> and never on a documented one
     * that is absent, so a two-field fixture published a two-row table and a
     * green build \u2014 while the guide's own prose described {@code timezoneId},
     * which the table did not carry.
     */
    @Test
    void patchDetails() throws Exception {
        authenticate();

        mockMvc.perform(patch("/api/tour-operators/{id}", OPERATOR_ID)
                        .header("Authorization", "Bearer test-access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Acme Tours",
                                    "address": {
                                        "address1": "Calle Mayor 1",
                                        "address2": "3\u00ba B",
                                        "city": "Madrid",
                                        "province": "Madrid",
                                        "zip": "28013",
                                        "countryId": "11111111-1111-1111-1111-111111111111"
                                    },
                                    "phone": "+34 611 111 111",
                                    "email": "hola@acme.test",
                                    "timezoneId": "cccccccc-cccc-cccc-cccc-cccccccccccc",
                                    "currencyId": "cccc0001-0000-0000-0000-000000000001"
                                }"""))
                .andExpect(status().isNoContent())
                .andDo(document("tour-operators/update",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(parameterWithName("id").description("The tour operator id")),
                        requestFields(
                                fieldWithPath("name")
                                        .description("Absent = unchanged. Cannot be cleared, only replaced "
                                                + "(2\u2013150 chars)").optional(),
                                fieldWithPath("address")
                                        .description("Absent = unchanged. Sending it REPLACES the stored "
                                                + "address whole, optional parts included \u2014 send every "
                                                + "line you want kept, or a city-only body leaves a new "
                                                + "city on an old street").optional(),
                                fieldWithPath("address.address1")
                                        .description("Street line (required when address is sent, \u2264255)")
                                        .optional(),
                                fieldWithPath("address.address2")
                                        .description("Second line; omit to clear it").optional(),
                                fieldWithPath("address.city")
                                        .description("City (required when address is sent, \u2264120)")
                                        .optional(),
                                fieldWithPath("address.province")
                                        .description("Province or region; omit to clear it").optional(),
                                fieldWithPath("address.zip")
                                        .description("Postcode; omit to clear it").optional(),
                                fieldWithPath("address.countryId")
                                        .description("A reference country id (GET /api/countries); unknown "
                                                + "\u2192 422").optional(),
                                fieldWithPath("phone")
                                        .description("Absent = unchanged; blank = clear. \u226430 chars, "
                                                + "no format imposed").optional(),
                                fieldWithPath("email")
                                        .description("Absent = unchanged; blank = clear. \u2264320 chars")
                                        .optional(),
                                fieldWithPath("timezoneId")
                                        .description("A reference timezone id (GET /api/timezones). "
                                                + "CHANGING THIS RE-TIMES THE WHOLE PUBLISHED SCHEDULE: "
                                                + "departures store operator-local wall-clock with no "
                                                + "zone, so a 10:00 sailing stays \"10:00\" and now means "
                                                + "a different instant. Nothing rewrites the stored rows. "
                                                + "That is what correcting a mis-set timezone needs, and "
                                                + "it is wrong for an operator that actually moved").optional(),
                                fieldWithPath("currencyId")
                                        .description("A reference currency id (GET /api/currencies)")
                                        .optional())));
    }
}

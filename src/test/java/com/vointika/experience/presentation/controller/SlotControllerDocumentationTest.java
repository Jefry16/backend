package com.vointika.experience.presentation.controller;

import com.vointika.experience.application.dto.output.SlotView;
import com.vointika.experience.application.usecase.CancelSlotUseCase;
import com.vointika.experience.application.usecase.CreateSlotUseCase;
import com.vointika.experience.application.usecase.CreateSlotsUseCase;
import com.vointika.experience.application.usecase.GetSlotUseCase;
import com.vointika.experience.application.usecase.ListSlotsUseCase;
import com.vointika.experience.application.usecase.UpdateSlotUseCase;
import com.vointika.experience.domain.valueobject.SlotStatus;
import com.vointika.shared.exception.ConflictException;
import com.vointika.shared.exception.ForbiddenException;
import com.vointika.shared.exception.InvalidFieldException;
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
import com.vointika.shared.web.docs.ApiErrorSnippets;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.patch;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SlotController.class)
@ExtendWith(RestDocumentationExtension.class)
@Import({SecurityConfig.class, WebConfig.class, ListQueryParser.class})
class SlotControllerDocumentationTest {

    private static final String OP = "019f7f33-1833-7dc1-b008-47e6c68b3ea2";
    private static final String EXP = "aaaaaaaa-0000-4000-8000-000000000001";
    private static final String SLOT = "bbbbbbbb-0000-4000-8000-000000000001";
    private static final String AUD = "cccccccc-0000-4000-8000-000000000001";
    private static final String USER = "550e8400-e29b-41d4-a716-446655440000";
    private static final String TOKEN = "test-access-token";
    private static final String BEARER = "Bearer " + TOKEN;

    /**
     * <b>Dates are derived, not written down.</b> All three create examples shipped
     * with hardcoded dates that were future when typed and past by the time anyone
     * read them — so a reader copying the published request got
     * {@code "Date must be today or later"} rather than the endpoint's actual answer,
     * and the recurring-422 example could not produce the error it documents. The
     * snapshots differ per build; that costs nothing here, because generated snippets
     * are not in version control.
     */
    private static final LocalDate NEXT_MONTH = LocalDate.now().plusMonths(1).withDayOfMonth(1);

    /** A Tuesday and the Wednesday after it, so a Sunday pattern matches nothing. */
    private static final LocalDate TUESDAY = NEXT_MONTH.with(java.time.temporal.TemporalAdjusters.next(java.time.DayOfWeek.TUESDAY));

    private static final String SINGLE_BODY = """
            {"startAt":"%sT10:00","endAt":"%sT13:00",
             "audiencePrices":[{"audienceId":"cccccccc-0000-4000-8000-000000000001","price":30.00,"capacity":10}]}"""
            .formatted(NEXT_MONTH, NEXT_MONTH);

    private static final String RECURRING_BODY = """
            {"days":[1,3,5],"startTime":"10:00","endTime":"13:00",
             "validFrom":"%s","validTo":"%s",
             "audiencePrices":[{"audienceId":"cccccccc-0000-4000-8000-000000000001","price":30.00,"capacity":10}]}"""
            .formatted(NEXT_MONTH, NEXT_MONTH.plusMonths(1));

    /**
     * A Tuesday-to-Wednesday window asking for Sundays — so no date in it matches, and
     * the request reaches the day-matching check rather than tripping the earlier
     * "must be today or later" one.
     */
    private static final String RECURRING_BODY_MATCHING_NOTHING = """
            {"days":[0],"startTime":"10:00","endTime":"13:00",
             "validFrom":"%s","validTo":"%s",
             "audiencePrices":[{"audienceId":"cccccccc-0000-4000-8000-000000000001","price":30.00,"capacity":10}]}"""
            .formatted(TUESDAY, TUESDAY.plusDays(1));

    /** An id no slot has, so the 404 example is not the 200 example. */
    private static final String MISSING_SLOT = "bbbbbbbb-0000-4000-8000-000000000404";

    /** A slot already in its terminal state, for the conflict example. */
    private static final String CANCELLED_SLOT = "bbbbbbbb-0000-4000-8000-000000000409";

    private static final String STAFF_TOKEN = "staff-access-token";
    private static final String STAFF_BEARER = "Bearer " + STAFF_TOKEN;
    private static final String STAFF_USER = "550e8400-e29b-41d4-a716-4466554400ff";

    private static final String UPDATE_BODY = """
            {"capacities":[{"audienceId":"cccccccc-0000-4000-8000-000000000001","capacity":8}]}""";

    private MockMvc mockMvc;

    @MockitoBean private ListSlotsUseCase listSlotsUseCase;
    @MockitoBean private GetSlotUseCase getSlotUseCase;
    @MockitoBean private CreateSlotUseCase createSlotUseCase;
    @MockitoBean private CreateSlotsUseCase createSlotsUseCase;
    @MockitoBean private CancelSlotUseCase cancelSlotUseCase;
    @MockitoBean private UpdateSlotUseCase updateSlotUseCase;
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

    private void authenticatedAsStaff() {
        when(accessTokenValidator.isValid(STAFF_TOKEN)).thenReturn(true);
        when(accessTokenValidator.extractUserId(STAFF_TOKEN)).thenReturn(STAFF_USER);
    }

    private void authenticated() {
        when(accessTokenValidator.isValid(TOKEN)).thenReturn(true);
        when(accessTokenValidator.extractUserId(TOKEN)).thenReturn(USER);
    }

    private SlotView view() {
        return new SlotView(
                UUID.fromString(SLOT), UUID.fromString(EXP),
                LocalDateTime.of(2026, 8, 1, 10, 0), LocalDateTime.of(2026, 8, 1, 13, 0),
                6, 180, "Sunset Tour", "A guided walk", SlotStatus.AVAILABLE,
                Instant.parse("2026-07-25T10:00:00Z"),
                List.of(new SlotView.AudiencePricingItem(
                        UUID.fromString(AUD), "Adults", new BigDecimal("30.00"), 10, 1, 0)));
    }

    @Test
    void createSingle() throws Exception {
        authenticated();
        when(createSlotUseCase.execute(any())).thenReturn(UUID.fromString(SLOT));
        mockMvc.perform(post("/api/tour-operators/{id}/experiences/{eid}/slot", OP, EXP)
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content(SINGLE_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andDo(document("slots/create-single",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(parameterWithName("id").description("The tour operator id"), parameterWithName("eid").description("The experience the slot belongs to"))));
    }

    @Test
    void createRecurring() throws Exception {
        authenticated();
        mockMvc.perform(post("/api/tour-operators/{id}/experiences/{eid}/slots", OP, EXP)
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content(RECURRING_BODY))
                .andExpect(status().isCreated())
                .andDo(document("slots/create-recurring",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(parameterWithName("id").description("The tour operator id"), parameterWithName("eid").description("The experience the slot belongs to"))));
    }

    @Test
    void createRecurringRejectsAPatternThatMatchesNothing() throws Exception {
        // Sends the pattern that matches nothing, not the one that produces slots.
        authenticated();
        doThrow(new com.vointika.shared.exception.InvalidFieldException(
                "No date in the validity window falls on the selected days"))
                .when(createSlotsUseCase).execute(any());

        mockMvc.perform(post("/api/tour-operators/{id}/experiences/{eid}/slots", OP, EXP)
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content(RECURRING_BODY_MATCHING_NOTHING))
                .andExpect(status().isUnprocessableEntity())
                .andDo(document("slots/create-recurring-no-match",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(parameterWithName("id").description("The tour operator id"), parameterWithName("eid").description("The experience the slot belongs to")),
                        responseFields(ApiErrorSnippets.errorFields())));
    }

    @Test
    void list() throws Exception {
        authenticated();
        when(listSlotsUseCase.execute(any(), any())).thenReturn(new CursorPage<>(List.of(view()), null));
        mockMvc.perform(get("/api/tour-operators/{id}/slots", OP)
                        .header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].context").value("slots"))
                .andExpect(jsonPath("$.data[0].durationMinutes").value(180))
                .andDo(document("slots/list",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(parameterWithName("id").description("The tour operator id")),
                        responseFields(
                                fieldWithPath("data[].id").description("The slot id"),
                                fieldWithPath("data[].context").description("The entity's collection: \"slots\""),
                                fieldWithPath("data[].experienceId").description("The experience this departure belongs to"),
                                fieldWithPath("data[].experienceName").description("Snapshotted at create and kept in sync when the experience is edited"),
                                fieldWithPath("data[].experienceDescription").description("Snapshotted alongside the name").optional(),
                                fieldWithPath("data[].startAt").description("Operator-LOCAL wall-clock start — no zone. A 10:00 sailing stays 10:00 if the operator changes timezone"),
                                fieldWithPath("data[].endAt").description("Operator-local wall-clock end; may cross midnight"),
                                fieldWithPath("data[].day").description("Day of week, 0–6, Sunday-first"),
                                fieldWithPath("data[].durationMinutes").description("Derived from start and end; never stored"),
                                fieldWithPath("data[].status").description("AVAILABLE, SOLD_OUT or CANCELLED. None is an operator toggle. **SOLD_OUT is not written yet** — it is derived from bookings counted at checkout, and checkout does not exist, so no live slot reports it. CANCELLED has its own endpoint"),
                                fieldWithPath("data[].audiencePrices[].audienceId").description("The pricing tier"),
                                fieldWithPath("data[].audiencePrices[].audienceName").description("Frozen at create; kept in sync when the audience is renamed"),
                                fieldWithPath("data[].audiencePrices[].price").description("Frozen at create — editing the audience never repriced a sold departure"),
                                fieldWithPath("data[].audiencePrices[].capacity").description("Seats offered to this tier"),
                                fieldWithPath("data[].audiencePrices[].paxPerUnit").description("People per unit sold"),
                                fieldWithPath("data[].audiencePrices[].bookedCount").description("Seats taken; capacity may never be set below it"),
                                fieldWithPath("nextCursor").type(JsonFieldType.STRING).description("Opaque cursor; null on the last page").optional())));
    }

    @Test
    void getOne() throws Exception {
        authenticated();
        when(getSlotUseCase.execute(any(), any(), any())).thenReturn(view());
        mockMvc.perform(get("/api/tour-operators/{id}/slots/{slotId}", OP, SLOT)
                        .header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.context").value("slots"))
                .andDo(document("slots/get",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(parameterWithName("id").description("The tour operator id"), parameterWithName("slotId").description("The slot id")),
                        responseFields(
                                fieldWithPath("id").description("The slot id"),
                                fieldWithPath("context").description("The entity's collection: \"slots\""),
                                fieldWithPath("experienceId").description("The experience this departure belongs to"),
                                fieldWithPath("experienceName").description("Snapshotted at create and kept in sync when the experience is edited"),
                                fieldWithPath("experienceDescription").description("Snapshotted alongside the name").optional(),
                                fieldWithPath("startAt").description("Operator-LOCAL wall-clock start — no zone. A 10:00 sailing stays 10:00 if the operator changes timezone"),
                                fieldWithPath("endAt").description("Operator-local wall-clock end; may cross midnight"),
                                fieldWithPath("day").description("Day of week, 0–6, Sunday-first"),
                                fieldWithPath("durationMinutes").description("Derived from start and end; never stored"),
                                fieldWithPath("status").description("AVAILABLE, SOLD_OUT or CANCELLED. None is an operator toggle. **SOLD_OUT is not written yet** — it is derived from bookings counted at checkout, and checkout does not exist, so no live slot reports it. CANCELLED has its own endpoint"),
                                fieldWithPath("audiencePrices[].audienceId").description("The pricing tier"),
                                fieldWithPath("audiencePrices[].audienceName").description("Frozen at create; kept in sync when the audience is renamed"),
                                fieldWithPath("audiencePrices[].price").description("Frozen at create — editing the audience never repriced a sold departure"),
                                fieldWithPath("audiencePrices[].capacity").description("Seats offered to this tier"),
                                fieldWithPath("audiencePrices[].paxPerUnit").description("People per unit sold"),
                                fieldWithPath("audiencePrices[].bookedCount").description("Seats taken; capacity may never be set below it"))));
    }

    @Test
    void cancel() throws Exception {
        authenticated();
        when(cancelSlotUseCase.execute(any(), any(), any())).thenReturn(view());
        mockMvc.perform(post("/api/tour-operators/{id}/slots/{slotId}/cancel", OP, SLOT)
                        .header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andDo(document("slots/cancel",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(parameterWithName("id").description("The tour operator id"), parameterWithName("slotId").description("The slot id")),
                        responseFields(
                                fieldWithPath("id").description("The slot id"),
                                fieldWithPath("context").description("The entity's collection: \"slots\""),
                                fieldWithPath("experienceId").description("The experience this departure belongs to"),
                                fieldWithPath("experienceName").description("Snapshotted at create and kept in sync when the experience is edited"),
                                fieldWithPath("experienceDescription").description("Snapshotted alongside the name").optional(),
                                fieldWithPath("startAt").description("Operator-LOCAL wall-clock start — no zone. A 10:00 sailing stays 10:00 if the operator changes timezone"),
                                fieldWithPath("endAt").description("Operator-local wall-clock end; may cross midnight"),
                                fieldWithPath("day").description("Day of week, 0–6, Sunday-first"),
                                fieldWithPath("durationMinutes").description("Derived from start and end; never stored"),
                                fieldWithPath("status").description("AVAILABLE, SOLD_OUT or CANCELLED. None is an operator toggle. **SOLD_OUT is not written yet** — it is derived from bookings counted at checkout, and checkout does not exist, so no live slot reports it. CANCELLED has its own endpoint"),
                                fieldWithPath("audiencePrices[].audienceId").description("The pricing tier"),
                                fieldWithPath("audiencePrices[].audienceName").description("Frozen at create; kept in sync when the audience is renamed"),
                                fieldWithPath("audiencePrices[].price").description("Frozen at create — editing the audience never repriced a sold departure"),
                                fieldWithPath("audiencePrices[].capacity").description("Seats offered to this tier"),
                                fieldWithPath("audiencePrices[].paxPerUnit").description("People per unit sold"),
                                fieldWithPath("audiencePrices[].bookedCount").description("Seats taken; capacity may never be set below it"))));
    }

    @Test
    void update() throws Exception {
        authenticated();
        when(updateSlotUseCase.execute(any(), any(), any(), any())).thenReturn(view());
        mockMvc.perform(patch("/api/tour-operators/{id}/slots/{slotId}", OP, SLOT)
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content(UPDATE_BODY))
                .andExpect(status().isOk())
                .andDo(document("slots/update",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(parameterWithName("id").description("The tour operator id"), parameterWithName("slotId").description("The slot id")),
                        // Capacity only. Timing is immutable — cancel and recreate —
                        // and status is not an operator choice.
                        requestFields(
                                fieldWithPath("capacities[].audienceId").description("A tier already priced on this slot; any other is a 422"),
                                fieldWithPath("capacities[].capacity").description("Seats for that tier. Never below bookedCount — that is a 422, not a truncation")),
                        responseFields(
                                fieldWithPath("id").description("The slot id"),
                                fieldWithPath("context").description("The entity's collection: \"slots\""),
                                fieldWithPath("experienceId").description("The experience this departure belongs to"),
                                fieldWithPath("experienceName").description("Snapshotted at create and kept in sync when the experience is edited"),
                                fieldWithPath("experienceDescription").description("Snapshotted alongside the name").optional(),
                                fieldWithPath("startAt").description("Operator-LOCAL wall-clock start — no zone. A 10:00 sailing stays 10:00 if the operator changes timezone"),
                                fieldWithPath("endAt").description("Operator-local wall-clock end; may cross midnight"),
                                fieldWithPath("day").description("Day of week, 0–6, Sunday-first"),
                                fieldWithPath("durationMinutes").description("Derived from start and end; never stored"),
                                fieldWithPath("status").description("AVAILABLE, SOLD_OUT or CANCELLED. None is an operator toggle. **SOLD_OUT is not written yet** — it is derived from bookings counted at checkout, and checkout does not exist, so no live slot reports it. CANCELLED has its own endpoint"),
                                fieldWithPath("audiencePrices[].audienceId").description("The pricing tier"),
                                fieldWithPath("audiencePrices[].audienceName").description("Frozen at create; kept in sync when the audience is renamed"),
                                fieldWithPath("audiencePrices[].price").description("Frozen at create — editing the audience never repriced a sold departure"),
                                fieldWithPath("audiencePrices[].capacity").description("Seats offered to this tier"),
                                fieldWithPath("audiencePrices[].paxPerUnit").description("People per unit sold"),
                                fieldWithPath("audiencePrices[].bookedCount").description("Seats taken; capacity may never be set below it"))));
    }

    @Test
    void createRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/tour-operators/{id}/experiences/{eid}/slot", OP, EXP)
                        .contentType(MediaType.APPLICATION_JSON).content(SINGLE_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void staffCannotCreate() throws Exception {
        authenticatedAsStaff();
        doThrow(new ForbiddenException("This action requires ADMIN privileges")).when(createSlotUseCase).execute(any());
        mockMvc.perform(post("/api/tour-operators/{id}/experiences/{eid}/slot", OP, EXP)
                        .header("Authorization", STAFF_BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content(SINGLE_BODY))
                .andExpect(status().isForbidden())
                .andDo(document("slots/create-single-forbidden",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(parameterWithName("id").description("The tour operator id"), parameterWithName("eid").description("The experience the slot belongs to")),
                        responseFields(ApiErrorSnippets.errorFields())));
    }

    @Test
    void cancelAlreadyCancelledIsConflict() throws Exception {
        // A different slot from the one the 200 example cancels: this one is already
        // in its terminal state, which is what the 409 is about.
        authenticated();
        doThrow(new ConflictException("Slot is already cancelled"))
                .when(cancelSlotUseCase).execute(any(), any(), any());
        mockMvc.perform(post("/api/tour-operators/{id}/slots/{slotId}/cancel", OP, CANCELLED_SLOT)
                        .header("Authorization", BEARER))
                .andExpect(status().isConflict())
                .andDo(document("slots/cancel-conflict",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(parameterWithName("id").description("The tour operator id"), parameterWithName("slotId").description("The slot id")),
                        responseFields(ApiErrorSnippets.errorFields())));
    }

    @Test
    void nonMemberGets404() throws Exception {
        authenticatedAsStaff();
        doThrow(new com.vointika.shared.exception.ResourceNotFoundException("not found"))
                .when(membershipCheck).ensureMember(eq(UUID.fromString(STAFF_USER)), eq(UUID.fromString(OP)));
        mockMvc.perform(get("/api/tour-operators/{id}/slots", OP)
                        .header("Authorization", STAFF_BEARER))
                .andExpect(status().isNotFound())
                .andDo(document("slots/list-not-found",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(parameterWithName("id").description("The tour operator id")),
                        responseFields(ApiErrorSnippets.errorFields())));
    }
    /**
     * <b>The 422 an operator actually meets.</b> Reducing seats on a departure that has
     * already sold is refused rather than truncated — the seats are booked, so the
     * capacity cannot go under them. This rule was named as a finding before it was
     * asserted; it is asserted now.
     */
    @Test
    void capacityBelowBookedIs422() throws Exception {
        authenticated();
        doThrow(new InvalidFieldException("Capacity cannot be below the seats already booked"))
                .when(updateSlotUseCase).execute(any(), any(), any(), any());

        mockMvc.perform(patch("/api/tour-operators/{id}/slots/{slotId}", OP, SLOT)
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"capacities":[{"audienceId":"cccccccc-0000-4000-8000-000000000001","capacity":1}]}"""))
                .andExpect(status().isUnprocessableEntity())
                .andDo(document("slots/update-capacity-too-low",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(parameterWithName("id").description("The tour operator id"), parameterWithName("slotId").description("The slot id")),
                        responseFields(ApiErrorSnippets.errorFields())));
    }

    /** The slot 404 the MISSING_SLOT fixture was written for. */
    @Test
    void unknownSlotIs404() throws Exception {
        authenticated();
        doThrow(new com.vointika.shared.exception.ResourceNotFoundException("Slot not found"))
                .when(getSlotUseCase).execute(any(), any(), any());

        mockMvc.perform(get("/api/tour-operators/{id}/slots/{slotId}", OP, MISSING_SLOT)
                        .header("Authorization", BEARER))
                .andExpect(status().isNotFound())
                .andDo(document("slots/get-not-found",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(parameterWithName("id").description("The tour operator id"), parameterWithName("slotId").description("The slot id")),
                        responseFields(ApiErrorSnippets.errorFields())));
    }

}

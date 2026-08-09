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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.patch;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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

    private static final String SINGLE_BODY = """
            {"startAt":"2026-08-01T10:00","endAt":"2026-08-01T13:00",
             "audiencePrices":[{"audienceId":"cccccccc-0000-4000-8000-000000000001","price":30.00,"capacity":10}]}""";
    private static final String RECURRING_BODY = """
            {"days":[1,3,5],"startTime":"10:00","endTime":"13:00",
             "validFrom":"2026-08-01","validTo":"2026-09-01",
             "audiencePrices":[{"audienceId":"cccccccc-0000-4000-8000-000000000001","price":30.00,"capacity":10}]}""";
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
        mockMvc.perform(post("/api/tour-operators/{id}/experiences/{eid}/slot", OP, EXP).with(csrf())
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content(SINGLE_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andDo(document("slots/create-single"));
    }

    @Test
    void createRecurring() throws Exception {
        authenticated();
        mockMvc.perform(post("/api/tour-operators/{id}/experiences/{eid}/slots", OP, EXP).with(csrf())
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content(RECURRING_BODY))
                .andExpect(status().isCreated())
                .andDo(document("slots/create-recurring"));
    }

    @Test
    void createRecurringRejectsAPatternThatMatchesNothing() throws Exception {
        authenticated();
        doThrow(new com.vointika.shared.exception.InvalidFieldException(
                "No date in the validity window falls on the selected days"))
                .when(createSlotsUseCase).execute(any());

        mockMvc.perform(post("/api/tour-operators/{id}/experiences/{eid}/slots", OP, EXP).with(csrf())
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content(RECURRING_BODY))
                .andExpect(status().isUnprocessableEntity());
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
                .andDo(document("slots/list"));
    }

    @Test
    void getOne() throws Exception {
        authenticated();
        when(getSlotUseCase.execute(any(), any(), any())).thenReturn(view());
        mockMvc.perform(get("/api/tour-operators/{id}/slots/{slotId}", OP, SLOT)
                        .header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.context").value("slots"))
                .andDo(document("slots/get"));
    }

    @Test
    void cancel() throws Exception {
        authenticated();
        when(cancelSlotUseCase.execute(any(), any(), any())).thenReturn(view());
        mockMvc.perform(post("/api/tour-operators/{id}/slots/{slotId}/cancel", OP, SLOT).with(csrf())
                        .header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andDo(document("slots/cancel"));
    }

    @Test
    void update() throws Exception {
        authenticated();
        when(updateSlotUseCase.execute(any(), any(), any(), any())).thenReturn(view());
        mockMvc.perform(patch("/api/tour-operators/{id}/slots/{slotId}", OP, SLOT).with(csrf())
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content(UPDATE_BODY))
                .andExpect(status().isOk())
                .andDo(document("slots/update"));
    }

    @Test
    void createRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/tour-operators/{id}/experiences/{eid}/slot", OP, EXP).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(SINGLE_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void staffCannotCreate() throws Exception {
        authenticated();
        doThrow(new ForbiddenException("admin")).when(createSlotUseCase).execute(any());
        mockMvc.perform(post("/api/tour-operators/{id}/experiences/{eid}/slot", OP, EXP).with(csrf())
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content(SINGLE_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void cancelAlreadyCancelledIsConflict() throws Exception {
        authenticated();
        doThrow(new ConflictException("Slot is already cancelled"))
                .when(cancelSlotUseCase).execute(any(), any(), any());
        mockMvc.perform(post("/api/tour-operators/{id}/slots/{slotId}/cancel", OP, SLOT).with(csrf())
                        .header("Authorization", BEARER))
                .andExpect(status().isConflict());
    }

    @Test
    void nonMemberGets404() throws Exception {
        authenticated();
        doThrow(new com.vointika.shared.exception.ResourceNotFoundException("not found"))
                .when(membershipCheck).ensureMember(eq(UUID.fromString(USER)), eq(UUID.fromString(OP)));
        mockMvc.perform(get("/api/tour-operators/{id}/slots", OP)
                        .header("Authorization", BEARER))
                .andExpect(status().isNotFound());
    }
}

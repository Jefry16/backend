package com.vointika.pickup.presentation.controller;

import com.vointika.pickup.application.usecase.CreatePickupLocationUseCase;
import com.vointika.pickup.application.usecase.DeletePickupLocationUseCase;
import com.vointika.pickup.application.usecase.GetPickupLocationUseCase;
import com.vointika.pickup.application.usecase.ListPickupLocationsUseCase;
import com.vointika.pickup.application.usecase.UpdatePickupLocationUseCase;
import com.vointika.pickup.domain.entity.PickupLocation;
import com.vointika.pickup.domain.valueobject.PickupLocationName;
import com.vointika.pickup.domain.valueobject.PickupLocationTime;
import com.vointika.shared.exception.ForbiddenException;
import com.vointika.shared.exception.ResourceAlreadyExistsException;
import com.vointika.shared.exception.ResourceNotFoundException;
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

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.patch;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PickupLocationController.class)
@ExtendWith(RestDocumentationExtension.class)
@Import({SecurityConfig.class, WebConfig.class, ListQueryParser.class})
class PickupLocationControllerDocumentationTest {

    private static final String OP = "019f7f33-1833-7dc1-b008-47e6c68b3ea2";
    private static final String PICKUP = "dddddddd-0000-4000-8000-000000000001";
    private static final String USER = "550e8400-e29b-41d4-a716-446655440000";
    private static final String TOKEN = "test-access-token";
    private static final String BEARER = "Bearer " + TOKEN;
    private static final String BODY = "{\"name\":\"Old Port\",\"time\":\"09:30\"}";

    private MockMvc mockMvc;

    @MockitoBean private CreatePickupLocationUseCase createPickupLocationUseCase;
    @MockitoBean private UpdatePickupLocationUseCase updatePickupLocationUseCase;
    @MockitoBean private ListPickupLocationsUseCase listPickupLocationsUseCase;
    @MockitoBean private GetPickupLocationUseCase getPickupLocationUseCase;
    @MockitoBean private DeletePickupLocationUseCase deletePickupLocationUseCase;
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

    private PickupLocation pickup() {
        return new PickupLocation(UUID.fromString(PICKUP), UUID.fromString(OP),
                new PickupLocationName("Old Port"), new PickupLocationTime(LocalTime.of(9, 30)),
                UUID.fromString(USER), Instant.parse("2026-07-25T10:00:00Z"));
    }

    @Test
    void create() throws Exception {
        authenticated();
        when(createPickupLocationUseCase.execute(any(), any(), any())).thenReturn(UUID.fromString(PICKUP));
        mockMvc.perform(post("/api/tour-operators/{id}/pickup-locations", OP).with(csrf())
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andDo(document("pickup-locations/create"));
    }

    @Test
    void list() throws Exception {
        authenticated();
        when(listPickupLocationsUseCase.execute(any(), any()))
                .thenReturn(new CursorPage<>(List.of(pickup()), null));
        mockMvc.perform(get("/api/tour-operators/{id}/pickup-locations", OP)
                        .header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].context").value("pickup-locations"))
                .andExpect(jsonPath("$.data[0].name").value("Old Port"))
                .andDo(document("pickup-locations/list"));
    }

    @Test
    void getOne() throws Exception {
        authenticated();
        when(getPickupLocationUseCase.execute(any(), any(), any())).thenReturn(pickup());
        mockMvc.perform(get("/api/tour-operators/{id}/pickup-locations/{pickupLocationId}", OP, PICKUP)
                        .header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.context").value("pickup-locations"))
                .andDo(document("pickup-locations/get"));
    }

    @Test
    void update() throws Exception {
        authenticated();
        mockMvc.perform(patch("/api/tour-operators/{id}/pickup-locations/{pickupLocationId}", OP, PICKUP).with(csrf())
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isNoContent())
                .andDo(document("pickup-locations/update"));
    }

    @Test
    void deleteOne() throws Exception {
        authenticated();
        mockMvc.perform(delete("/api/tour-operators/{id}/pickup-locations/{pickupLocationId}", OP, PICKUP).with(csrf())
                        .header("Authorization", BEARER))
                .andExpect(status().isNoContent())
                .andDo(document("pickup-locations/delete"));
    }

    @Test
    void createRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/tour-operators/{id}/pickup-locations", OP).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void staffCannotCreate() throws Exception {
        authenticated();
        doThrow(new ForbiddenException("admin")).when(createPickupLocationUseCase).execute(any(), any(), any());
        mockMvc.perform(post("/api/tour-operators/{id}/pickup-locations", OP).with(csrf())
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void duplicateNameIsConflict() throws Exception {
        authenticated();
        doThrow(new ResourceAlreadyExistsException("exists"))
                .when(createPickupLocationUseCase).execute(any(), any(), any());
        mockMvc.perform(post("/api/tour-operators/{id}/pickup-locations", OP).with(csrf())
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict());
    }

    @Test
    void nonMemberGets404() throws Exception {
        authenticated();
        doThrow(new ResourceNotFoundException("not found"))
                .when(membershipCheck).ensureMember(eq(UUID.fromString(USER)), eq(UUID.fromString(OP)));
        mockMvc.perform(get("/api/tour-operators/{id}/pickup-locations", OP)
                        .header("Authorization", BEARER))
                .andExpect(status().isNotFound());
    }
}

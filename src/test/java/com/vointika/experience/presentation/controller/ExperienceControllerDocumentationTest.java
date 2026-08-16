package com.vointika.experience.presentation.controller;

import java.math.BigDecimal;
import com.vointika.experience.application.dto.output.ExperienceView;
import com.vointika.experience.application.usecase.CreateExperienceUseCase;
import com.vointika.experience.application.usecase.GetExperienceUseCase;
import com.vointika.experience.application.usecase.ListExperiencesUseCase;
import com.vointika.experience.application.usecase.PublishExperienceUseCase;
import com.vointika.experience.application.usecase.UnpublishExperienceUseCase;
import com.vointika.experience.application.usecase.UpdateExperienceUseCase;
import com.vointika.shared.exception.ForbiddenException;
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
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders;
import org.springframework.restdocs.payload.JsonFieldType;

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

@WebMvcTest(ExperienceController.class)
@ExtendWith(RestDocumentationExtension.class)
@Import({SecurityConfig.class, WebConfig.class, ListQueryParser.class})
class ExperienceControllerDocumentationTest {

    private static final String OP = "019f7f33-1833-7dc1-b008-47e6c68b3ea2";
    private static final String EXP = "aaaaaaaa-0000-4000-8000-000000000001";
    private static final String USER = "550e8400-e29b-41d4-a716-446655440000";

    private MockMvc mockMvc;

    @MockitoBean private CreateExperienceUseCase createExperienceUseCase;
    @MockitoBean private ListExperiencesUseCase listExperiencesUseCase;
    @MockitoBean private GetExperienceUseCase getExperienceUseCase;
    @MockitoBean private UpdateExperienceUseCase updateExperienceUseCase;
    @MockitoBean private PublishExperienceUseCase publishExperienceUseCase;
    @MockitoBean private UnpublishExperienceUseCase unpublishExperienceUseCase;
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
        when(accessTokenValidator.extractUserId("test-access-token")).thenReturn(USER);
    }

    private ExperienceView view() {
        return new ExperienceView(UUID.fromString(EXP), UUID.fromString(OP), UUID.fromString(USER),
                "Sunset Dive", "sunset-dive", "A guided reef dive at dusk", "Long description…",
                true, UUID.fromString("bbbbbbbb-0000-4000-8000-000000000001"),
                "https://media.staging.vointika.com/thumb.jpg",
                List.of(UUID.fromString("bbbbbbbb-0000-4000-8000-000000000002")),
                List.of("https://media.staging.vointika.com/1.jpg"),
                24, false, "Sunset Dive | Acme Tours", "A guided reef dive at dusk, daily.",
                new BigDecimal("35.00"), Instant.parse("2026-07-21T10:00:00Z"));
    }

    private static final String CREATE_BODY = """
            {
              "name": "Sunset Dive",
              "description": "A guided reef dive at dusk",
              "longDescription": "Long description…",
              "featured": true,
              "mediaIds": [],
              "thumbnailMediaId": null,
              "bookingCutoffHours": 24
            }""";

    @Test
    void create() throws Exception {
        authenticated();
        when(createExperienceUseCase.execute(any(), any(), any())).thenReturn(UUID.fromString(EXP));

        mockMvc.perform(post("/api/tour-operators/{id}/experiences", OP)
                        .header("Authorization", "Bearer test-access-token")
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andDo(document("experiences/create",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        requestFields(
                                fieldWithPath("name").description("Display name (1–200)"),
                                fieldWithPath("description").description("Short description (≤500)"),
                                fieldWithPath("longDescription").description("Long description (≤10000)"),
                                fieldWithPath("featured").description("Featured flag (default false)").optional(),
                                fieldWithPath("mediaIds").description("Gallery media ids — must be in this operator's library (≤20)").optional(),
                                fieldWithPath("thumbnailMediaId").description("Thumbnail — must be one of mediaIds").optional(),
                                fieldWithPath("bookingCutoffHours").description("Advance-notice hours (≥0)")),
                        responseHeaders(headerWithName("Location").description("URI of the created experience"))));
    }

    @Test
    void list() throws Exception {
        authenticated();
        when(listExperiencesUseCase.execute(any(), any()))
                .thenReturn(new CursorPage<>(List.of(view()), "eyJ2MSI6Im5leHQifQ"));

        mockMvc.perform(get("/api/tour-operators/{id}/experiences", OP)
                        .header("Authorization", "Bearer test-access-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].context").value("experiences"))
                .andExpect(jsonPath("$.data[0].published").value(false))
                .andDo(document("experiences/list",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        responseFields(
                                fieldWithPath("data[].id").description("The experience id"),
                                fieldWithPath("data[].context").description("The entity's collection: \"experiences\""),
                                fieldWithPath("data[].name").description("Display name"),
                                fieldWithPath("data[].handle").description("Canonical handle (immutable; unique per operator across canonical and localized handles)"),
                                fieldWithPath("data[].description").description("Short description"),
                                fieldWithPath("data[].longDescription").description("Long description"),
                                fieldWithPath("data[].featured").description("Featured flag"),
                                fieldWithPath("data[].thumbnailMediaId").description("Thumbnail media id, or null").optional(),
                                fieldWithPath("data[].thumbnailUrl").description("Resolved thumbnail URL, or null").optional(),
                                fieldWithPath("data[].mediaIds").description("Gallery media ids (stored order)"),
                                fieldWithPath("data[].galleryUrls").description("Resolved gallery URLs (media-id order)"),
                                fieldWithPath("data[].bookingCutoffHours").description("Advance-notice hours"),
                                fieldWithPath("data[].published").description("Whether the experience is published (shopper-visible)"),
                                fieldWithPath("data[].seoTitle").description("Search-engine title override, or null — falls back to the operator's").optional(),
                                fieldWithPath("data[].seoDescription").description("Meta description override, or null — falls back to the operator's").optional(),
                                fieldWithPath("data[].startingPrice").description("The operator's advertised \"from\" price. Required and greater than 0 on every experience, drafts included; not derived from slot prices, so it can differ from the cheapest bookable tier."),
                                fieldWithPath("data[].createdBy").description("Creator user id"),
                                fieldWithPath("data[].createdAt").description("When created"),
                                fieldWithPath("nextCursor").type(JsonFieldType.STRING).description("Opaque cursor; null on the last page"))));
    }

    @Test
    void getExperience() throws Exception {
        authenticated();
        when(getExperienceUseCase.execute(any(), any(), any())).thenReturn(view());

        mockMvc.perform(get("/api/tour-operators/{id}/experiences/{experienceId}", OP, EXP)
                        .header("Authorization", "Bearer test-access-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.context").value("experiences"))
                // The bug this endpoint had: the request accepted these two and the
                // response never returned them, so an editor loaded the form without
                // them and the next save — a whole replace — cleared both.
                .andExpect(jsonPath("$.seoTitle").value("Sunset Dive | Acme Tours"))
                .andExpect(jsonPath("$.seoDescription").value("A guided reef dive at dusk, daily."))
                .andDo(document("experiences/get",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(
                                parameterWithName("id").description("The tour operator id"),
                                parameterWithName("experienceId").description("The experience id"))));
    }

    @Test
    void update() throws Exception {
        authenticated();
        mockMvc.perform(patch("/api/tour-operators/{id}/experiences/{experienceId}", OP, EXP)
                        .header("Authorization", "Bearer test-access-token")
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isNoContent())
                .andDo(document("experiences/update",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(
                                parameterWithName("id").description("The tour operator id"),
                                parameterWithName("experienceId").description("The experience id"))));
    }

    @Test
    void publish() throws Exception {
        authenticated();
        mockMvc.perform(post("/api/tour-operators/{id}/experiences/{experienceId}/publish", OP, EXP)
                        .header("Authorization", "Bearer test-access-token"))
                .andExpect(status().isNoContent())
                .andDo(document("experiences/publish",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(
                                parameterWithName("id").description("The tour operator id"),
                                parameterWithName("experienceId").description("The experience id"))));
    }

    @Test
    void unpublish() throws Exception {
        authenticated();
        mockMvc.perform(post("/api/tour-operators/{id}/experiences/{experienceId}/unpublish", OP, EXP)
                        .header("Authorization", "Bearer test-access-token"))
                .andExpect(status().isNoContent())
                .andDo(document("experiences/unpublish",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(
                                parameterWithName("id").description("The tour operator id"),
                                parameterWithName("experienceId").description("The experience id"))));
    }

    @Test
    void createRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/tour-operators/{id}/experiences", OP)
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void staffCannotCreate() throws Exception {
        authenticated();
        doThrow(new ForbiddenException("This action requires ADMIN privileges"))
                .when(createExperienceUseCase).execute(any(), any(), any());
        mockMvc.perform(post("/api/tour-operators/{id}/experiences", OP)
                        .header("Authorization", "Bearer test-access-token")
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void nonMemberGets404FromTheInterceptor() throws Exception {
        authenticated();
        doThrow(new ResourceNotFoundException("Tour operator not found"))
                .when(membershipCheck).ensureMember(eq(UUID.fromString(USER)), eq(UUID.fromString(OP)));
        mockMvc.perform(get("/api/tour-operators/{id}/experiences", OP)
                        .header("Authorization", "Bearer test-access-token"))
                .andExpect(status().isNotFound());
    }
}

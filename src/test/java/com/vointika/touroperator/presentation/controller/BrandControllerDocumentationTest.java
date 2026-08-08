package com.vointika.touroperator.presentation.controller;

import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.port.AccessTokenValidatorPort;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.web.security.SecurityConfig;
import com.vointika.touroperator.application.dto.output.BrandView;
import com.vointika.touroperator.application.usecase.GetBrandUseCase;
import com.vointika.touroperator.application.usecase.UpdateBrandUseCase;
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

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.http.HttpDocumentation.httpRequest;
import static org.springframework.restdocs.http.HttpDocumentation.httpResponse;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
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

@WebMvcTest(BrandController.class)
@ExtendWith(RestDocumentationExtension.class)
@Import({SecurityConfig.class, WebConfig.class})
class BrandControllerDocumentationTest {

    private static final String OPERATOR_ID = "019f7f33-1833-7dc1-b008-47e6c68b3ea2";
    private static final String USER_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final String MEDIA_ID = "019f8100-0000-7000-8000-000000000001";
    private static final String BODY = """
            {"slogan":"Sail the coast, not the crowds.",
             "shortDescription":"Small-group sailing out of Madrid since 2011.",
             "logoMediaId":"019f8100-0000-7000-8000-000000000001",
             "squareLogoMediaId":null,"faviconMediaId":null,"coverImageMediaId":null,
             "colors":{"primary":[{"background":"#0b3d5c","foreground":"#ffffff"}],
                       "secondary":[{"background":"#1c7ba8","foreground":"#000000"}]},
             "socialLinks":[{"platform":"INSTAGRAM","url":"https://instagram.com/acme"}]}
            """;

    private MockMvc mockMvc;

    @MockitoBean private GetBrandUseCase getUseCase;
    @MockitoBean private UpdateBrandUseCase updateUseCase;
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

    private static BrandView view() {
        return new BrandView(
                "Sail the coast, not the crowds.",
                "Small-group sailing out of Madrid since 2011.",
                UUID.fromString(MEDIA_ID), null, null, null,
                new BrandView.Colors(
                        List.of(new BrandView.Color("#0b3d5c", "#ffffff")),
                        List.of(new BrandView.Color("#1c7ba8", "#000000"))),
                List.of(new BrandView.SocialLink("INSTAGRAM", "https://instagram.com/acme")));
    }

    @Test
    void getOne() throws Exception {
        authenticated();
        when(getUseCase.execute(any(), any())).thenReturn(view());

        mockMvc.perform(get("/api/tour-operators/{id}/brand", OPERATOR_ID)
                        .header("Authorization", "Bearer test-access-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.colors.primary[0].background").value("#0b3d5c"))
                .andDo(document("tour-operators/brand/get",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(parameterWithName("id").description("The tour operator id")),
                        responseFields(
                                fieldWithPath("slogan").description("The brand's tagline, or null").optional(),
                                fieldWithPath("shortDescription")
                                        .description("A sentence or two, or null").optional(),
                                fieldWithPath("logoMediaId")
                                        .description("Media id, not a URL — the storefront resolves it")
                                        .optional(),
                                fieldWithPath("squareLogoMediaId").description("Media id, or null").optional(),
                                fieldWithPath("faviconMediaId").description("Media id, or null").optional(),
                                fieldWithPath("coverImageMediaId").description("Media id, or null").optional(),
                                fieldWithPath("colors.primary[].background")
                                        .description("Lower-case hex; order is significant").optional(),
                                fieldWithPath("colors.primary[].foreground").description("Lower-case hex").optional(),
                                fieldWithPath("colors.secondary[].background").description("Lower-case hex").optional(),
                                fieldWithPath("colors.secondary[].foreground").description("Lower-case hex").optional(),
                                fieldWithPath("socialLinks[].platform")
                                        .description("One of the closed platform set").optional(),
                                fieldWithPath("socialLinks[].url").description("http or https only").optional())));
    }

    @Test
    void anOperatorWithNoBrandGetsAnEmptyOneNotA404() throws Exception {
        authenticated();
        when(getUseCase.execute(any(), any())).thenReturn(new BrandView(
                null, null, null, null, null, null,
                new BrandView.Colors(List.of(), List.of()), List.of()));

        mockMvc.perform(get("/api/tour-operators/{id}/brand", OPERATOR_ID)
                        .header("Authorization", "Bearer test-access-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slogan").doesNotExist())
                .andExpect(jsonPath("$.socialLinks").isEmpty());
    }

    @Test
    void updateOne() throws Exception {
        authenticated();

        mockMvc.perform(put("/api/tour-operators/{id}/brand", OPERATOR_ID)
                        .header("Authorization", "Bearer test-access-token")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isNoContent())
                .andDo(document("tour-operators/brand/update",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(parameterWithName("id").description("The tour operator id")),
                        requestFields(
                                fieldWithPath("slogan")
                                        .description("≤80 chars; absent or blank clears it").optional(),
                                fieldWithPath("shortDescription")
                                        .description("≤150 chars; absent or blank clears it").optional(),
                                fieldWithPath("logoMediaId")
                                        .description("Must be in this operator's media library, else 422")
                                        .optional(),
                                fieldWithPath("squareLogoMediaId").description("Media id, or null").optional(),
                                fieldWithPath("faviconMediaId").description("Media id, or null").optional(),
                                fieldWithPath("coverImageMediaId").description("Media id, or null").optional(),
                                fieldWithPath("colors.primary[].background")
                                        .description("Six-digit hex; case-insensitive, stored lower-case. "
                                                + "Array order becomes the palette order a theme indexes")
                                        .optional(),
                                fieldWithPath("colors.primary[].foreground").description("Six-digit hex").optional(),
                                fieldWithPath("colors.secondary[].background").description("Six-digit hex").optional(),
                                fieldWithPath("colors.secondary[].foreground").description("Six-digit hex").optional(),
                                fieldWithPath("socialLinks[].platform")
                                        .description("One of the closed set; at most one entry per platform")
                                        .optional(),
                                fieldWithPath("socialLinks[].url")
                                        .description("http or https only — this is rendered into an href "
                                                + "on a public page").optional())));
    }

    @Test
    void mediaFromAnotherLibraryIsUnprocessable() throws Exception {
        authenticated();
        doThrow(new InvalidFieldException("Media not found in this operator's library"))
                .when(updateUseCase).execute(any(), any(), any());

        mockMvc.perform(put("/api/tour-operators/{id}/brand", OPERATOR_ID)
                        .header("Authorization", "Bearer test-access-token")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void anonymousIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/tour-operators/{id}/brand", OPERATOR_ID))
                .andExpect(status().isUnauthorized());
    }
}

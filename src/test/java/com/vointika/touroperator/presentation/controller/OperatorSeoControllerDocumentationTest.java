package com.vointika.touroperator.presentation.controller;

import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.port.AccessTokenValidatorPort;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.web.security.SecurityConfig;
import com.vointika.touroperator.application.dto.output.OperatorSeoView;
import com.vointika.touroperator.application.usecase.GetOperatorSeoUseCase;
import com.vointika.touroperator.application.usecase.UpdateOperatorSeoUseCase;
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

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
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
import static org.springframework.restdocs.http.HttpDocumentation.httpRequest;
import static org.springframework.restdocs.http.HttpDocumentation.httpResponse;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OperatorSeoController.class)
@ExtendWith(RestDocumentationExtension.class)
@Import({SecurityConfig.class, WebConfig.class})
class OperatorSeoControllerDocumentationTest {

    private static final String OPERATOR_ID = "019f7f33-1833-7dc1-b008-47e6c68b3ea2";
    private static final String USER_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final String MEDIA_ID = "aaaaaaaa-0000-4000-8000-000000000001";

    private MockMvc mockMvc;

    @MockitoBean private GetOperatorSeoUseCase getOperatorSeoUseCase;
    @MockitoBean private UpdateOperatorSeoUseCase updateOperatorSeoUseCase;
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

    @Test
    void getSeo() throws Exception {
        authenticated();
        when(getOperatorSeoUseCase.execute(any(), any())).thenReturn(new OperatorSeoView(
                "Acme Tours — diving in Santo Domingo",
                "Small-group diving and snorkelling on the south coast.",
                UUID.fromString(MEDIA_ID)));

        mockMvc.perform(get("/api/tour-operators/{id}/seo", OPERATOR_ID)
                        .header("Authorization", "Bearer test-access-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seoTitle").value("Acme Tours — diving in Santo Domingo"))
                .andDo(document("tour-operators/seo/get",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(parameterWithName("id").description("The tour operator id")),
                        responseFields(
                                fieldWithPath("seoTitle")
                                        .description("Canonical SEO title (≤70), or null").optional(),
                                fieldWithPath("seoDescription")
                                        .description("Canonical meta description (≤320), or null").optional(),
                                fieldWithPath("ogImageMediaId")
                                        .description("Media id of the social image, or null. The storefront "
                                                + "resolves it to a URL at read time").optional())));
    }

    @Test
    void updateSeo() throws Exception {
        authenticated();

        mockMvc.perform(put("/api/tour-operators/{id}/seo", OPERATOR_ID)
                        .header("Authorization", "Bearer test-access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"seoTitle\":\"Acme Tours — diving\","
                                + "\"seoDescription\":\"Small-group diving.\","
                                + "\"ogImageMediaId\":\"" + MEDIA_ID + "\"}"))
                .andExpect(status().isNoContent())
                .andDo(document("tour-operators/seo/update",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(parameterWithName("id").description("The tour operator id")),
                        requestFields(
                                fieldWithPath("seoTitle")
                                        .description("≤70 chars. A whole-value replace: null CLEARS the "
                                                + "override rather than leaving it").optional(),
                                fieldWithPath("seoDescription")
                                        .description("≤320 chars; null clears it").optional(),
                                fieldWithPath("ogImageMediaId")
                                        .description("A media id from this operator's library; "
                                                + "unknown or foreign → 422").optional())));
    }

    @Test
    void aForeignOgImageIsUnprocessable() throws Exception {
        authenticated();
        doThrow(new InvalidFieldException("Media not found in this operator's library"))
                .when(updateOperatorSeoUseCase).execute(any(), any(), any(), any(), any());

        mockMvc.perform(put("/api/tour-operators/{id}/seo", OPERATOR_ID)
                        .header("Authorization", "Bearer test-access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ogImageMediaId\":\"" + MEDIA_ID + "\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void seoRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/tour-operators/{id}/seo", OPERATOR_ID))
                .andExpect(status().isUnauthorized());
    }
}

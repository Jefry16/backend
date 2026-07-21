package com.vointika.touroperator.presentation.controller;

import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.port.AccessTokenValidatorPort;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.web.security.SecurityConfig;
import com.vointika.touroperator.application.usecase.ClearOperatorLogoUseCase;
import com.vointika.touroperator.application.usecase.SetOperatorLogoUseCase;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.http.HttpDocumentation.httpRequest;
import static org.springframework.restdocs.http.HttpDocumentation.httpResponse;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TourOperatorLogoController.class)
@ExtendWith(RestDocumentationExtension.class)
@Import({SecurityConfig.class, WebConfig.class})
class TourOperatorLogoControllerDocumentationTest {

    private static final String OPERATOR_ID = "019f7f33-1833-7dc1-b008-47e6c68b3ea2";
    private static final String MEDIA_ID = "aaaaaaaa-0000-4000-8000-000000000001";
    private static final String USER_ID = "550e8400-e29b-41d4-a716-446655440000";

    private MockMvc mockMvc;

    @MockitoBean private SetOperatorLogoUseCase setOperatorLogoUseCase;
    @MockitoBean private ClearOperatorLogoUseCase clearOperatorLogoUseCase;
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
    void setLogo() throws Exception {
        authenticated();

        mockMvc.perform(put("/api/tour-operators/{id}/logo", OPERATOR_ID)
                        .header("Authorization", "Bearer test-access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"mediaId\": \"" + MEDIA_ID + "\" }"))
                .andExpect(status().isNoContent())
                .andDo(document("tour-operators/logo/set",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(parameterWithName("id").description("The tour operator id")),
                        requestFields(fieldWithPath("mediaId")
                                .description("A media id from this operator's library; unknown media → 422"))));
    }

    @Test
    void clearLogo() throws Exception {
        authenticated();

        mockMvc.perform(delete("/api/tour-operators/{id}/logo", OPERATOR_ID)
                        .header("Authorization", "Bearer test-access-token"))
                .andExpect(status().isNoContent())
                .andDo(document("tour-operators/logo/clear",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(parameterWithName("id").description("The tour operator id"))));
    }

    @Test
    void setLogoRequiresAuthentication() throws Exception {
        mockMvc.perform(put("/api/tour-operators/{id}/logo", OPERATOR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"mediaId\": \"" + MEDIA_ID + "\" }"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unknownMediaIsUnprocessable() throws Exception {
        authenticated();
        doThrow(new InvalidFieldException("Media not found in this operator's library"))
                .when(setOperatorLogoUseCase).execute(any(), any(), any());

        mockMvc.perform(put("/api/tour-operators/{id}/logo", OPERATOR_ID)
                        .header("Authorization", "Bearer test-access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"mediaId\": \"" + MEDIA_ID + "\" }"))
                .andExpect(status().isUnprocessableEntity());
    }
}

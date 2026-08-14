package com.vointika.metafield.presentation.controller;

import com.vointika.metafield.application.usecase.DeleteMetafieldTranslationsUseCase;
import com.vointika.metafield.application.usecase.GetMetafieldTranslationsUseCase;
import com.vointika.metafield.application.usecase.ListMetafieldTranslationLocalesUseCase;
import com.vointika.metafield.application.usecase.UpsertMetafieldTranslationsUseCase;
import com.vointika.metafield.domain.valueobject.MetafieldOwnerType;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Per-locale overlays for the operator's own metafields — the read that makes {@code tourOperator.metafields} speak the visitor's language.
 */
@WebMvcTest(TourOperatorMetafieldTranslationController.class)
@ExtendWith(RestDocumentationExtension.class)
@Import({SecurityConfig.class, WebConfig.class, ListQueryParser.class})
class TourOperatorMetafieldTranslationControllerDocumentationTest {

    private static final String OP = "019f7f33-1833-7dc1-b008-47e6c68b3ea2";
    private static final String USER = "550e8400-e29b-41d4-a716-446655440000";
    private static final String TOKEN = "test-access-token";
    private static final String BEARER = "Bearer " + TOKEN;

    private MockMvc mockMvc;

    @MockitoBean private ListMetafieldTranslationLocalesUseCase listLocalesUseCase;
    @MockitoBean private GetMetafieldTranslationsUseCase getUseCase;
    @MockitoBean private UpsertMetafieldTranslationsUseCase upsertUseCase;
    @MockitoBean private DeleteMetafieldTranslationsUseCase deleteUseCase;
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
        when(accessTokenValidator.isValid(TOKEN)).thenReturn(true);
        when(accessTokenValidator.extractUserId(TOKEN)).thenReturn(USER);
    }

    @Test
    void listLocales() throws Exception {
        when(listLocalesUseCase.execute(any(), any(), eq(MetafieldOwnerType.TOUR_OPERATOR), any()))
                .thenReturn(List.of("en", "fr"));

        mockMvc.perform(get("/api/tour-operators/{id}/metafield-translations", OP)
                        .header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("en"))
                .andDo(document("tour-operator-metafield-translations/list-locales",
                        requestHeaders(headerWithName("Authorization")
                                .description("Bearer access token"))));
    }

    @Test
    void getOne() throws Exception {
        when(getUseCase.execute(any(), any(), eq(MetafieldOwnerType.TOUR_OPERATOR), any(), anyString()))
                .thenReturn(Map.of("custom.opening-hours", "Mon-Fri 09:00-18:00"));

        mockMvc.perform(get("/api/tour-operators/{id}/metafield-translations/{locale}", OP, "en")
                        .header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['custom.opening-hours']").value("Mon-Fri 09:00-18:00"))
                .andDo(document("tour-operator-metafield-translations/get",
                        requestHeaders(headerWithName("Authorization")
                                .description("Bearer access token"))));
    }

    @Test
    void upsert() throws Exception {
        mockMvc.perform(put("/api/tour-operators/{id}/metafield-translations/{locale}", OP, "en")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"values\":{\"custom.opening-hours\":\"Mon-Fri 09:00-18:00\"}}"))
                .andExpect(status().isNoContent())
                .andDo(document("tour-operator-metafield-translations/upsert",
                        requestHeaders(headerWithName("Authorization")
                                .description("Bearer access token"))));
    }

    @Test
    void deleteLocale() throws Exception {
        mockMvc.perform(delete("/api/tour-operators/{id}/metafield-translations/{locale}", OP, "en")
                        .header("Authorization", BEARER))
                .andExpect(status().isNoContent())
                .andDo(document("tour-operator-metafield-translations/delete",
                        requestHeaders(headerWithName("Authorization")
                                .description("Bearer access token"))));
    }
}

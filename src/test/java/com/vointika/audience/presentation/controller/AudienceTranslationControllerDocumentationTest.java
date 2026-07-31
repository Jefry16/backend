package com.vointika.audience.presentation.controller;

import com.vointika.audience.application.usecase.DeleteAudienceTranslationUseCase;
import com.vointika.audience.application.usecase.GetAudienceTranslationUseCase;
import com.vointika.audience.application.usecase.ListAudienceTranslationsUseCase;
import com.vointika.audience.application.usecase.UpsertAudienceTranslationUseCase;
import com.vointika.audience.domain.entity.AudienceTranslation;
import com.vointika.audience.domain.valueobject.AudienceName;
import com.vointika.shared.exception.ForbiddenException;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.port.AccessTokenValidatorPort;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.valueobject.LocaleCode;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AudienceTranslationController.class)
@ExtendWith(RestDocumentationExtension.class)
@Import({SecurityConfig.class, WebConfig.class, ListQueryParser.class})
class AudienceTranslationControllerDocumentationTest {

    private static final String OP = "019f7f33-1833-7dc1-b008-47e6c68b3ea2";
    private static final String AUD = "aaaaaaaa-0000-4000-8000-000000000001";
    private static final String USER = "550e8400-e29b-41d4-a716-446655440000";
    private static final String TOKEN = "test-access-token";
    private static final String BEARER = "Bearer " + TOKEN;
    private static final String BODY = "{\"name\":\"Adultos\"}";

    private MockMvc mockMvc;

    @MockitoBean private UpsertAudienceTranslationUseCase upsertUseCase;
    @MockitoBean private GetAudienceTranslationUseCase getUseCase;
    @MockitoBean private ListAudienceTranslationsUseCase listUseCase;
    @MockitoBean private DeleteAudienceTranslationUseCase deleteUseCase;
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

    private AudienceTranslation translation() {
        return new AudienceTranslation(UUID.fromString(AUD), UUID.fromString(OP),
                new LocaleCode("es"), new AudienceName("Adultos"));
    }

    @Test
    void list() throws Exception {
        authenticated();
        when(listUseCase.execute(any(), any(), any())).thenReturn(List.of(translation()));
        mockMvc.perform(get("/api/tour-operators/{id}/audiences/{audienceId}/translations", OP, AUD)
                        .header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].locale").value("es"))
                .andExpect(jsonPath("$[0].name").value("Adultos"))
                .andDo(document("audience-translations/list"));
    }

    @Test
    void getOne() throws Exception {
        authenticated();
        when(getUseCase.execute(any(), any(), any(), any())).thenReturn(translation());
        mockMvc.perform(get("/api/tour-operators/{id}/audiences/{audienceId}/translations/{locale}", OP, AUD, "es")
                        .header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locale").value("es"))
                .andDo(document("audience-translations/get"));
    }

    @Test
    void upsert() throws Exception {
        authenticated();
        mockMvc.perform(put("/api/tour-operators/{id}/audiences/{audienceId}/translations/{locale}", OP, AUD, "es")
                        .with(csrf())
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isNoContent())
                .andDo(document("audience-translations/upsert"));
    }

    @Test
    void deleteOne() throws Exception {
        authenticated();
        mockMvc.perform(delete("/api/tour-operators/{id}/audiences/{audienceId}/translations/{locale}", OP, AUD, "es")
                        .with(csrf())
                        .header("Authorization", BEARER))
                .andExpect(status().isNoContent())
                .andDo(document("audience-translations/delete"));
    }

    @Test
    void upsertRequiresAuthentication() throws Exception {
        mockMvc.perform(put("/api/tour-operators/{id}/audiences/{audienceId}/translations/{locale}", OP, AUD, "es")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void staffCannotUpsert() throws Exception {
        authenticated();
        doThrow(new ForbiddenException("admin"))
                .when(upsertUseCase).execute(any(), any(), any(), any(), any());
        mockMvc.perform(put("/api/tour-operators/{id}/audiences/{audienceId}/translations/{locale}", OP, AUD, "es")
                        .with(csrf())
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void aMissingBodyIs400BeforeTheHandlerRuns() throws Exception {
        authenticated();

        // The handler dropped its `body == null` guard because @RequestBody is
        // required by default — this is what makes that guard unreachable.
        mockMvc.perform(put("/api/tour-operators/{id}/audiences/{audienceId}/translations/{locale}", OP, AUD, "es")
                        .with(csrf())
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(upsertUseCase);
    }

    @Test
    void unsupportedLocaleIs422() throws Exception {
        authenticated();
        doThrow(new InvalidFieldException("unsupported"))
                .when(upsertUseCase).execute(any(), any(), any(), any(), any());
        mockMvc.perform(put("/api/tour-operators/{id}/audiences/{audienceId}/translations/{locale}", OP, AUD, "fr")
                        .with(csrf())
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnprocessableEntity());
    }
}

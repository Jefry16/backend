package com.vointika.contact.presentation.controller;

import com.vointika.contact.application.usecase.SubmitContactMessageUseCase;
import com.vointika.contact.infrastructure.security.ContactPublicRoutes;
import com.vointika.shared.exception.TooManyRequestsException;
import com.vointika.shared.port.AccessTokenValidatorPort;
import com.vointika.shared.web.security.InternalApiSecretFilter;
import com.vointika.shared.web.security.SecurityConfig;
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
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ContactIntakeController.class,
        properties = "app.internal.shared-secret=test-internal-secret")
@ExtendWith(RestDocumentationExtension.class)
@Import({SecurityConfig.class, ContactPublicRoutes.class})
class ContactIntakeControllerDocumentationTest {

    private static final String SLUG = "acme";
    private static final String SECRET = "test-internal-secret";
    private static final String BODY = """
            {"name":"Laura Pérez","email":"laura@example.com",\
            "summary":"Child seats on the sunset tour?",\
            "content":"We are a family of four. Do you provide child seats?"}""";

    private MockMvc mockMvc;

    @MockitoBean private SubmitContactMessageUseCase submitUseCase;
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

    @Test
    void submitContactMessage() throws Exception {
        mockMvc.perform(post("/api/internal/storefront/{tenantSlug}/contact-messages", SLUG)
                        .header(InternalApiSecretFilter.HEADER_NAME, SECRET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY)
                        .with(csrf()))
                .andExpect(status().isNoContent())
                .andDo(document("contact-submit-message",
                        requestHeaders(
                                headerWithName(InternalApiSecretFilter.HEADER_NAME)
                                        .description("Shared secret authenticating the storefront BFF")),
                        requestFields(
                                fieldWithPath("name")
                                        .description("The sender's name — optional; blank is stored as none")
                                        .optional(),
                                fieldWithPath("email").description("Reply address, required"),
                                fieldWithPath("summary").description("Subject, required, ≤200 chars"),
                                fieldWithPath("content").description("The message, required, ≤5000 chars"))));
    }

    @Test
    void rejectsACallWithoutTheSharedSecret() throws Exception {
        mockMvc.perform(post("/api/internal/storefront/{tenantSlug}/contact-messages", SLUG)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY)
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void aThrottledSubmissionIsTooManyRequests() throws Exception {
        doThrow(new TooManyRequestsException("Too many messages, please try again later"))
                .when(submitUseCase).execute(any(), any(), any(), any(), any());

        mockMvc.perform(post("/api/internal/storefront/{tenantSlug}/contact-messages", SLUG)
                        .header(InternalApiSecretFilter.HEADER_NAME, SECRET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY)
                        .with(csrf()))
                .andExpect(status().isTooManyRequests());
    }
}

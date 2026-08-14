package com.vointika.contact.presentation.controller;

import com.vointika.contact.application.usecase.DeleteContactMessageUseCase;
import com.vointika.contact.application.usecase.GetContactMessageUseCase;
import com.vointika.contact.application.usecase.ListContactMessagesUseCase;
import com.vointika.contact.domain.entity.ContactMessage;
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
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ContactMessageController.class)
@ExtendWith(RestDocumentationExtension.class)
@Import({SecurityConfig.class, WebConfig.class, ListQueryParser.class})
class ContactMessageControllerDocumentationTest {

    private static final String OP = "019f7f33-1833-7dc1-b008-47e6c68b3ea2";
    private static final String MSG = "cccccccc-0000-4000-8000-000000000002";
    private static final String USER = "550e8400-e29b-41d4-a716-446655440000";
    private static final String TOKEN = "test-access-token";
    private static final String BEARER = "Bearer " + TOKEN;

    private MockMvc mockMvc;

    @MockitoBean private ListContactMessagesUseCase listUseCase;
    @MockitoBean private GetContactMessageUseCase getUseCase;
    @MockitoBean private DeleteContactMessageUseCase deleteUseCase;
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

    private ContactMessage message() {
        return new ContactMessage(UUID.fromString(MSG), UUID.fromString(OP),
                "Laura Pérez", "laura@example.com",
                "Do you have child seats on the sunset tour?",
                "Hi! We are a family of four (kids are 4 and 7). Do you provide child-size life vests?",
                Instant.parse("2026-07-28T10:00:00Z"));
    }

    @Test
    void list() throws Exception {
        authenticated();
        when(listUseCase.execute(any(), any()))
                .thenReturn(new CursorPage<>(List.of(message()), null));

        mockMvc.perform(get("/api/tour-operators/{id}/contact-messages", OP)
                        .header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].context").value("contact-messages"))
                .andDo(document("contact-messages/list",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        responseFields(
                                fieldWithPath("data[].id").description("The message id"),
                                fieldWithPath("data[].context").description("\"contact-messages\""),
                                fieldWithPath("data[].name").type("String").description("The shopper's name, or null").optional(),
                                fieldWithPath("data[].email").description("The shopper's reply address"),
                                fieldWithPath("data[].summary").description("One-line subject"),
                                fieldWithPath("data[].createdAt").description("When submitted"),
                                fieldWithPath("nextCursor").description("Opaque cursor; null on the last page").optional())));
    }

    @Test
    void getOne() throws Exception {
        authenticated();
        when(getUseCase.execute(eq(UUID.fromString(OP)), eq(UUID.fromString(MSG)), any()))
                .thenReturn(message());

        mockMvc.perform(get("/api/tour-operators/{id}/contact-messages/{messageId}", OP, MSG)
                        .header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("laura@example.com"))
                .andDo(document("contact-messages/get",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        responseFields(
                                fieldWithPath("id").description("The message id"),
                                fieldWithPath("context").description("\"contact-messages\""),
                                fieldWithPath("name").type("String").description("The shopper's name, or null").optional(),
                                fieldWithPath("email").description("The shopper's reply address"),
                                fieldWithPath("summary").description("One-line subject"),
                                fieldWithPath("content").description("The raw message body, verbatim"),
                                fieldWithPath("createdAt").description("When submitted"))));
    }


    @Test
    void deleteOne() throws Exception {
        authenticated();

        mockMvc.perform(delete("/api/tour-operators/{id}/contact-messages/{messageId}", OP, MSG)
                        .header("Authorization", BEARER))
                .andExpect(status().isNoContent())
                .andDo(document("contact-messages/delete",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token"))));
    }
}

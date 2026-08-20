package com.vointika.contact.presentation.controller;

import com.vointika.contact.domain.repository.ContactMessageRepository;
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
import com.vointika.shared.exception.ForbiddenException;
import com.vointika.shared.web.docs.ApiErrorSnippets;
import com.vointika.shared.exception.ResourceNotFoundException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.restdocs.payload.JsonFieldType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
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

    /**
     * An id no message has, so a published 404 example is not byte-identical to the
     * 200 example above it. Reusing {@link #MSG} showed the same curl under both
     * headings with nothing to say what differed.
     */
    private static final String MISSING_MSG = "cccccccc-0000-4000-8000-000000000404";

    /**
     * A STAFF member's token. The 403 turns on <em>who</em> is asking, not on which
     * message, so this is what makes the forbidden example differ from the 204 —
     * the URL is necessarily the same one.
     */
    private static final String STAFF_TOKEN = "staff-access-token";
    private static final String STAFF_BEARER = "Bearer " + STAFF_TOKEN;
    private static final String STAFF_USER = "550e8400-e29b-41d4-a716-4466554400ff";

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

    private void authenticatedAsStaff() {
        when(accessTokenValidator.isValid(STAFF_TOKEN)).thenReturn(true);
        when(accessTokenValidator.extractUserId(STAFF_TOKEN)).thenReturn(STAFF_USER);
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
                        pathParameters(parameterWithName("id").description("The tour operator id")),
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        responseFields(
                                fieldWithPath("data[].id").description("The message id — UUIDv7, so descending id is newest-first"),
                                fieldWithPath("data[].context").description("The entity's collection: \"contact-messages\""),
                                fieldWithPath("data[].name").description("Who wrote the message. Always present"),
                                fieldWithPath("data[].email").description("The sender's reply address"),
                                fieldWithPath("data[].summary").description("One-line subject"),
                                fieldWithPath("data[].createdAt").description("When submitted"),
                                fieldWithPath("nextCursor").type(JsonFieldType.STRING).description("Opaque cursor; null on the last page").optional())));
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
                        pathParameters(
                                parameterWithName("id").description("The tour operator id"),
                                parameterWithName("messageId").description("The message id")),
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        responseFields(
                                fieldWithPath("id").description("The message id — UUIDv7, so descending id is newest-first"),
                                fieldWithPath("context").description("The entity's collection: \"contact-messages\""),
                                fieldWithPath("name").description("Who wrote the message. Always present"),
                                fieldWithPath("email").description("The sender's reply address"),
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
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(
                                parameterWithName("id").description("The tour operator id"),
                                parameterWithName("messageId").description("The message id"))));
    }

    /**
     * The error half of this endpoint. A message id that does not exist and one
     * belonging to another operator answer identically — the lookup is scoped to
     * the operator, so the two are indistinguishable on purpose.
     */
    @Test
    void getUnknownIs404() throws Exception {
        authenticated();
        when(getUseCase.execute(any(), any(), any()))
                .thenThrow(new ResourceNotFoundException(ContactMessageRepository.NOT_FOUND));

        mockMvc.perform(get("/api/tour-operators/{id}/contact-messages/{messageId}", OP, MISSING_MSG)
                        .header("Authorization", BEARER))
                .andExpect(status().isNotFound())
                // The one assertion holding this sentence. Both stubs now derive it, so the
                // guide follows a reword — and that alone leaves the wording pinned nowhere
                // while two published bodies carry it (PATTERNS §9a).
                .andExpect(jsonPath("$.message").value("Contact message not found"))
                .andDo(document("contact-messages/get-not-found",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(
                                parameterWithName("id").description("The tour operator id"),
                                parameterWithName("messageId").description("A message id that does not exist, or belongs to another operator")),
                        responseFields(ApiErrorSnippets.errorFields())));
    }

    /**
     * <b>The one a STAFF integrator actually hits.</b> Reading the inbox is open to
     * any member and deleting is ADMIN+, so a STAFF caller can list a message and
     * then be refused when deleting it. Nothing in the guide said so in a form a
     * client could read.
     */
    @Test
    void deleteAsStaffIs403() throws Exception {
        authenticatedAsStaff();
        doThrow(new ForbiddenException(TourOperatorMembershipCheck.requiresRoleMessage("ADMIN")))
                .when(deleteUseCase).execute(any(), any(), any());

        mockMvc.perform(delete("/api/tour-operators/{id}/contact-messages/{messageId}", OP, MSG)
                        .header("Authorization", STAFF_BEARER))
                .andExpect(status().isForbidden())
                .andDo(document("contact-messages/delete-forbidden",
                        requestHeaders(headerWithName("Authorization").description("A STAFF member's bearer token — the same request from an ADMIN returns 204")),
                        pathParameters(
                                parameterWithName("id").description("The tour operator id"),
                                parameterWithName("messageId").description("The message id")),
                        responseFields(ApiErrorSnippets.errorFields())));
    }

    /**
     * DELETE has the same 404 the reads do, and it is the ordinary concurrent case:
     * two admins with the inbox open, one deletes, the other's DELETE misses. The
     * role check runs first, so this is what an ADMIN sees.
     */
    @Test
    void deleteUnknownIs404() throws Exception {
        authenticated();
        doThrow(new ResourceNotFoundException(ContactMessageRepository.NOT_FOUND))
                .when(deleteUseCase).execute(any(), any(), any());

        mockMvc.perform(delete("/api/tour-operators/{id}/contact-messages/{messageId}", OP, MISSING_MSG)
                        .header("Authorization", BEARER))
                .andExpect(status().isNotFound())
                .andDo(document("contact-messages/delete-not-found",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(
                                parameterWithName("id").description("The tour operator id"),
                                parameterWithName("messageId").description("A message id that does not exist, or belongs to another operator")),
                        responseFields(ApiErrorSnippets.errorFields())));
    }

}

package com.vointika.touroperator.presentation.controller;

import com.vointika.shared.exception.ConflictException;
import com.vointika.shared.exception.ForbiddenException;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.web.docs.ApiErrorSnippets;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.port.AccessTokenValidatorPort;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.web.list.ListQueryParser;
import com.vointika.shared.web.security.SecurityConfig;
import com.vointika.touroperator.application.dto.output.MemberListView;
import com.vointika.touroperator.application.usecase.ChangeMemberRoleUseCase;
import com.vointika.touroperator.application.usecase.GetMemberUseCase;
import com.vointika.touroperator.application.usecase.ListMembersUseCase;
import com.vointika.touroperator.application.usecase.RemoveTeamMemberUseCase;
import com.vointika.touroperator.domain.enums.MemberRole;
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
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.patch;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TeamMemberController.class)
@ExtendWith(RestDocumentationExtension.class)
@Import({SecurityConfig.class, WebConfig.class, ListQueryParser.class})
class TeamMemberControllerDocumentationTest {

    private static final String OP = "019f7f33-1833-7dc1-b008-47e6c68b3ea2";
    private static final String USER = "550e8400-e29b-41d4-a716-446655440000";
    private static final String OTHER_USER = "7c9e6679-7425-40de-944b-e07fc1f90ae7";
    private static final String OWNER_USER = "3f2504e0-4f89-41d3-9a0c-0305e82c3301";
    private static final String MISSING_OP = "019f7f33-0000-7dc1-b008-000000000000";

    private MockMvc mockMvc;

    @MockitoBean private ListMembersUseCase listMembersUseCase;
    @MockitoBean private GetMemberUseCase getMemberUseCase;
    @MockitoBean private ChangeMemberRoleUseCase changeMemberRoleUseCase;
    @MockitoBean private RemoveTeamMemberUseCase removeTeamMemberUseCase;
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

    @Test
    void listMembers() throws Exception {
        authenticated();
        when(listMembersUseCase.execute(any(), eq(UUID.fromString(USER))))
                .thenReturn(new CursorPage<>(List.of(new MemberListView(
                        UUID.fromString(USER), MemberRole.OWNER,
                        Instant.parse("2026-01-01T00:00:00Z"), "Olive Owner", "owner@example.com")),
                        "eyJ2MSI6Im5leHQifQ"));

        mockMvc.perform(get("/api/tour-operators/{id}/members", OP)
                        .header("Authorization", "Bearer test-access-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].context").value("users"))
                .andExpect(jsonPath("$.data[0].role").value("OWNER"))
                .andExpect(jsonPath("$.nextCursor").value("eyJ2MSI6Im5leHQifQ"))
                .andDo(document("tour-operators/members/list",
                        pathParameters(parameterWithName("id").description("The tour operator id")),
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        responseFields(
                                fieldWithPath("data[].id").description("The member's user id"),
                                fieldWithPath("data[].context").description("The entity's collection: \"users\""),
                                fieldWithPath("data[].role").description("OWNER, ADMIN, or STAFF"),
                                fieldWithPath("data[].joinedAt").description("When they joined"),
                                fieldWithPath("data[].name").description("Display name (best-effort; may be null)"),
                                fieldWithPath("data[].email").description("Email (best-effort; may be null)"),
                                fieldWithPath("nextCursor").type(JsonFieldType.STRING).description("Opaque cursor for the next page; null on the last page"))));
    }

    @Test
    void getMember() throws Exception {
        authenticated();
        when(getMemberUseCase.execute(any(), any(), eq(UUID.fromString(USER))))
                .thenReturn(new MemberListView(
                        UUID.fromString(USER), MemberRole.ADMIN,
                        Instant.parse("2026-01-05T10:00:00Z"), "Grace Hopper", "grace@acme.test"));

        mockMvc.perform(get("/api/tour-operators/{id}/members/{userId}", OP, USER)
                        .header("Authorization", "Bearer test-access-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.context").value("users"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andDo(document("tour-operators/members/get",
                        pathParameters(parameterWithName("id").description("The tour operator id"),
                                parameterWithName("userId").description("The member's user id; a user who is not a member of this operator is a 404")),
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        responseFields(
                                fieldWithPath("id").description("The member's user id"),
                                fieldWithPath("context").description("The entity's collection: \"users\""),
                                fieldWithPath("role").description("OWNER, ADMIN, or STAFF"),
                                fieldWithPath("joinedAt").description("When they joined"),
                                fieldWithPath("name").description("Display name (best-effort; may be null)"),
                                fieldWithPath("email").description("Email (best-effort; may be null)"))));
    }

    @Test
    void changeRole() throws Exception {
        authenticated();
        mockMvc.perform(patch("/api/tour-operators/{id}/members/{userId}", OP, OTHER_USER)
                        .header("Authorization", "Bearer test-access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"role\": \"ADMIN\" }"))
                .andExpect(status().isNoContent())
                .andDo(document("tour-operators/members/change-role",
                        pathParameters(parameterWithName("id").description("The tour operator id"),
                                parameterWithName("userId").description("The member whose role changes; your own id is a 409")),
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        requestFields(fieldWithPath("role")
                                .description("OWNER, ADMIN or STAFF. OWNER is not a role you grant — it is a "
                                        + "TRANSFER: the target becomes OWNER and YOU are demoted to ADMIN in "
                                        + "the same transaction, and only the new owner can hand it back. "
                                        + "Only an owner may send it. Anything else → 422"))));
    }

    /**
     * The 403 that only an owner clears. {@code ensureAdmin} has already passed
     * here — an ADMIN may change any ordinary member's role and is refused on the
     * owner's, which is a boundary <em>inside</em> a permission an admin has.
     */
    @Test
    void anAdminCannotChangeTheOwnersRole() throws Exception {
        authenticated();
        doThrow(new ForbiddenException(ChangeMemberRoleUseCase.OWNER_ONLY))
                .when(changeMemberRoleUseCase).execute(any(), any(), any(), any());

        mockMvc.perform(patch("/api/tour-operators/{id}/members/{userId}", OP, OWNER_USER)
                        .header("Authorization", "Bearer test-access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"role\": \"STAFF\" }"))
                .andExpect(status().isForbidden())
                .andDo(document("tour-operators/members/change-role-forbidden",
                        pathParameters(parameterWithName("id").description("The tour operator id"),
                                parameterWithName("userId").description("The owner's user id")),
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        responseFields(ApiErrorSnippets.errorFields())));
    }

    /**
     * Nobody edits their own role, whatever it is. The example uses the caller's
     * own id, because that is what the error turns on.
     *
     * <p><b>The body has to be a non-OWNER role for this message to be reachable.</b>
     * {@code role: OWNER} is gated before the transaction — an ADMIN sending it is
     * refused by {@code ensureOwner} (403) and never meets the self-check, and an
     * OWNER sending it is the sole owner by the single-owner index, so
     * {@code apply} throws the other variant, "Transfer ownership to another member
     * before changing your own role". An earlier revision published
     * {@code role: OWNER} against this message, which is a pair the code cannot
     * produce.
     */
    @Test
    void changingYourOwnRoleIs409() throws Exception {
        authenticated();
        doThrow(new ConflictException("You cannot change your own role"))
                .when(changeMemberRoleUseCase).execute(any(), any(), any(), any());

        mockMvc.perform(patch("/api/tour-operators/{id}/members/{userId}", OP, USER)
                        .header("Authorization", "Bearer test-access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"role\": \"ADMIN\" }"))
                .andExpect(status().isConflict())
                .andDo(document("tour-operators/members/change-role-conflict",
                        pathParameters(parameterWithName("id").description("The tour operator id"),
                                parameterWithName("userId").description("Your own user id — which is the conflict")),
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        responseFields(ApiErrorSnippets.errorFields())));
    }

    @Test
    void removeMember() throws Exception {
        authenticated();
        mockMvc.perform(delete("/api/tour-operators/{id}/members/{userId}", OP, OTHER_USER)
                        .header("Authorization", "Bearer test-access-token"))
                .andExpect(status().isNoContent())
                .andDo(document("tour-operators/members/remove",
                        pathParameters(parameterWithName("id").description("The tour operator id"),
                                parameterWithName("userId").description("The member to remove; your own id means leaving the team, which needs no role")),
                        requestHeaders(headerWithName("Authorization").description("Bearer access token"))));
    }

    /**
     * The wall an operator with one owner hits, from either side: the owner
     * cannot leave and an admin cannot remove them. The example uses the caller's
     * own id, so it publishes the "leaving" wording rather than the "removing" one.
     */
    @Test
    void theLastOwnerCannotLeave() throws Exception {
        authenticated();
        doThrow(new ConflictException("Transfer ownership to another member before leaving"))
                .when(removeTeamMemberUseCase).execute(any(), any(), any());

        mockMvc.perform(delete("/api/tour-operators/{id}/members/{userId}", OP, USER)
                        .header("Authorization", "Bearer test-access-token"))
                .andExpect(status().isConflict())
                .andDo(document("tour-operators/members/remove-conflict",
                        pathParameters(parameterWithName("id").description("The tour operator id"),
                                parameterWithName("userId").description("Your own user id — leaving the team")),
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        responseFields(ApiErrorSnippets.errorFields())));
    }

    @Test
    void nonMemberGets404FromTheInterceptor() throws Exception {
        authenticated();
        doThrow(new ResourceNotFoundException(TourOperatorMembershipCheck.TENANT_NOT_FOUND))
                .when(membershipCheck).ensureMember(eq(UUID.fromString(USER)), eq(UUID.fromString(MISSING_OP)));

        mockMvc.perform(get("/api/tour-operators/{id}/members", MISSING_OP)
                        .header("Authorization", "Bearer test-access-token"))
                .andExpect(status().isNotFound())
                .andDo(document("tour-operators/members/list-not-found",
                        pathParameters(parameterWithName("id").description(
                                "An operator you are not a member of, or one that does not exist — "
                                        + "the two answer identically")),
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        responseFields(ApiErrorSnippets.errorFields())));
    }
}

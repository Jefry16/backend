package com.vointika.touroperator.presentation.controller;

import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.AccessTokenValidatorPort;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.web.security.SecurityConfig;
import com.vointika.touroperator.application.dto.output.MemberListView;
import com.vointika.touroperator.application.usecase.ChangeMemberRoleUseCase;
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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

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
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TeamMemberController.class)
@ExtendWith(RestDocumentationExtension.class)
@Import({SecurityConfig.class, WebConfig.class})
class TeamMemberControllerDocumentationTest {

    private static final String OP = "019f7f33-1833-7dc1-b008-47e6c68b3ea2";
    private static final String USER = "550e8400-e29b-41d4-a716-446655440000";

    private MockMvc mockMvc;

    @MockitoBean private ListMembersUseCase listMembersUseCase;
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
        when(listMembersUseCase.execute(eq(UUID.fromString(OP)), eq(UUID.fromString(USER))))
                .thenReturn(List.of(new MemberListView(
                        UUID.fromString(USER), MemberRole.OWNER,
                        Instant.parse("2026-01-01T00:00:00Z"), "Olive Owner", "owner@example.com")));

        mockMvc.perform(get("/api/tour-operators/{id}/members", OP)
                        .header("Authorization", "Bearer test-access-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("members"))
                .andExpect(jsonPath("$[0].role").value("OWNER"))
                .andDo(document("tour-operators/members/list",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        responseFields(
                                fieldWithPath("[].userId").description("The member's user id"),
                                fieldWithPath("[].type").description("The resource type"),
                                fieldWithPath("[].role").description("OWNER, ADMIN, or STAFF"),
                                fieldWithPath("[].joinedAt").description("When they joined"),
                                fieldWithPath("[].name").description("Display name (best-effort; may be null)"),
                                fieldWithPath("[].email").description("Email (best-effort; may be null)"))));
    }

    @Test
    void changeRole() throws Exception {
        authenticated();
        mockMvc.perform(patch("/api/tour-operators/{id}/members/{userId}", OP, UUID.randomUUID())
                        .header("Authorization", "Bearer test-access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"role\": \"ADMIN\" }"))
                .andExpect(status().isNoContent())
                .andDo(document("tour-operators/members/change-role",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        requestFields(fieldWithPath("role")
                                .description("Target role: OWNER (ownership transfer), ADMIN, or STAFF"))));
    }

    @Test
    void removeMember() throws Exception {
        authenticated();
        mockMvc.perform(delete("/api/tour-operators/{id}/members/{userId}", OP, UUID.randomUUID())
                        .header("Authorization", "Bearer test-access-token"))
                .andExpect(status().isNoContent())
                .andDo(document("tour-operators/members/remove",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token"))));
    }

    @Test
    void nonMemberGets404FromTheInterceptor() throws Exception {
        authenticated();
        doThrow(new ResourceNotFoundException("Tour operator not found"))
                .when(membershipCheck).ensureMember(eq(UUID.fromString(USER)), eq(UUID.fromString(OP)));

        mockMvc.perform(get("/api/tour-operators/{id}/members", OP)
                        .header("Authorization", "Bearer test-access-token"))
                .andExpect(status().isNotFound());
    }
}

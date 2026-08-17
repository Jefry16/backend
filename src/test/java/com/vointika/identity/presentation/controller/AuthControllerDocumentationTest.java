package com.vointika.identity.presentation.controller;

import com.vointika.identity.application.dto.output.LoginUserOutput;
import com.vointika.identity.application.dto.output.GetProfileOutput;
import com.vointika.identity.application.dto.output.RefreshAccessTokenOutput;
import com.vointika.identity.application.dto.output.SetAvatarOutput;
import com.vointika.shared.web.security.RefreshTokenCookieFactory;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.exception.UnauthorizedException;
import com.vointika.shared.web.docs.ApiErrorSnippets;
import com.vointika.shared.port.AccessTokenValidatorPort;
import com.vointika.identity.application.usecase.*;
import com.vointika.identity.infrastructure.security.IdentityPublicRoutes;
import com.vointika.shared.web.security.SecurityConfig;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.vointika.shared.media.MediaUrlResolver;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.multipart;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.partWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.restdocs.request.RequestDocumentation.requestParts;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@ExtendWith(RestDocumentationExtension.class)
@Import({SecurityConfig.class, IdentityPublicRoutes.class, RefreshTokenCookieFactory.class})
class AuthControllerDocumentationTest {

    private static final String REFRESH_COOKIE_NAME = "vointika_refresh";

    private MockMvc mockMvc;

    @MockitoBean
    private RegisterUserUseCase registerUserUseCase;

    @MockitoBean
    private VerifyAccountUseCase verifyAccountUseCase;

    @MockitoBean
    private ResendVerificationEmailUseCase resendVerificationEmailUseCase;

    @MockitoBean
    private LoginUserUseCase loginUserUseCase;

    @MockitoBean
    private RefreshAccessTokenUseCase refreshAccessTokenUseCase;

    @MockitoBean
    private LogoutUserUseCase logoutUserUseCase;

    @MockitoBean
    private ChangePasswordUseCase changePasswordUseCase;

    @MockitoBean
    private RequestPasswordResetUseCase requestPasswordResetUseCase;

    @MockitoBean
    private ResetPasswordUseCase resetPasswordUseCase;

    @MockitoBean
    private GetProfileUseCase getProfileUseCase;

    @MockitoBean
    private SetAvatarUseCase setAvatarUseCase;

    @MockitoBean
    private ClearAvatarUseCase clearAvatarUseCase;

    @MockitoBean
    private ChangeLanguageUseCase changeLanguageUseCase;

    @MockitoBean
    private AccessTokenValidatorPort accessTokenValidator;

    @MockitoBean
    private MediaUrlResolver mediaUrlResolver;



    @BeforeEach
    void setUp(WebApplicationContext context, RestDocumentationContextProvider restDocumentation) {
        lenient().when(mediaUrlResolver.toUrl(any())).thenAnswer(inv -> inv.getArgument(0));
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(documentationConfiguration(restDocumentation)
                        .operationPreprocessors()
                        .withRequestDefaults(prettyPrint())
                        .withResponseDefaults(prettyPrint()))
                .apply(springSecurity())
                .build();
    }

    @Test
    void register() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "user@example.com",
                                    "name": "John Doe",
                                    "password": "Password1!",
                                    "language": "es"
                                }
                                """))
                .andExpect(status().isCreated())
                .andDo(document("auth/register",
                        requestFields(
                                fieldWithPath("email").description("The user's email address (max 255 chars, valid email format; normalized to lower case and trimmed)"),
                                fieldWithPath("name").description("The user's display name (2-100 chars after trimming)"),
                                fieldWithPath("password").description("The user's password (8 chars min, 72 bytes max; must include uppercase, lowercase, digit, and special character)"),
                                fieldWithPath("language").description("Optional: the frontend's current UI language (locale code, e.g. \"es\"); stored as the new user's preference and used to localize the verification email. Unsupported or absent defaults to \"en\".").optional()
                        )));
    }

    @Test
    void verifyAccount() throws Exception {
        mockMvc.perform(get("/api/auth/verify")
                        .param("token", "abc123-verification-token"))
                .andExpect(status().isNoContent())
                .andDo(document("auth/verify",
                        queryParameters(
                                parameterWithName("token").description("The email verification token")
                        )));
    }

    @Test
    void resendVerification() throws Exception {
        mockMvc.perform(post("/api/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "user@example.com"}
                                """))
                .andExpect(status().isNoContent())
                .andDo(document("auth/resend-verification",
                        requestFields(
                                fieldWithPath("email").description("The email address to resend the verification to")
                        )));
    }

    @Test
    void login() throws Exception {
        when(loginUserUseCase.execute(any()))
                .thenReturn(new LoginUserOutput(
                        "eyJhbGciOiJIUzI1NiJ9.access.token",
                        "raw-refresh-token-value"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "user@example.com",
                                    "password": "Password1!"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString(REFRESH_COOKIE_NAME + "=raw-refresh-token-value")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("SameSite=Strict")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Path=/api/auth")))
                .andDo(document("auth/login",
                        requestFields(
                                fieldWithPath("email").description("The user's email address (max 255 chars, valid email format; normalized to lower case and trimmed)"),
                                fieldWithPath("password").description("The user's password")
                        ),
                        responseHeaders(
                                headerWithName("Set-Cookie").description(
                                        "Refresh-token cookie (HttpOnly, Secure, SameSite=Strict, Path=/api/auth, Max-Age=30d). " +
                                        "Frontend never reads this; the browser will send it back automatically on /api/auth/refresh and /api/auth/logout. " +
                                        "Cross-origin requests must use credentials: 'include'.")
                        ),
                        responseFields(
                                fieldWithPath("accessToken").description("JWT access token (expires in 15 minutes)")
                        )));
    }

    @Test
    void refreshAccessToken() throws Exception {
        when(refreshAccessTokenUseCase.execute(any()))
                .thenReturn(new RefreshAccessTokenOutput(
                        "eyJhbGciOiJIUzI1NiJ9.new-access.token",
                        "rotated-refresh-token-value"));

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie(REFRESH_COOKIE_NAME, "current-refresh-token-value")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString(REFRESH_COOKIE_NAME + "=rotated-refresh-token-value")))
                .andDo(document("auth/refresh",
                        responseHeaders(
                                headerWithName("Set-Cookie").description(
                                        "New rotated refresh-token cookie. The previous refresh token is now revoked; " +
                                        "presenting it again triggers reuse detection and revokes the entire session family.")
                        ),
                        responseFields(
                                fieldWithPath("accessToken").description("A new JWT access token")
                        )));
    }

    @Test
    void logout() throws Exception {
        when(accessTokenValidator.isValid("test-access-token")).thenReturn(true);
        when(accessTokenValidator.extractUserId("test-access-token"))
                .thenReturn("550e8400-e29b-41d4-a716-446655440000");

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer test-access-token")
                        .cookie(new Cookie(REFRESH_COOKIE_NAME, "current-refresh-token-value")))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")))
                .andDo(document("auth/logout",
                        requestHeaders(
                                headerWithName("Authorization").description("Bearer access token")
                        ),
                        responseHeaders(
                                headerWithName("Set-Cookie").description("Cleared refresh-token cookie (Max-Age=0). The entire session family is revoked server-side.")
                        )));
    }

    @Test
    void changePassword() throws Exception {
        when(accessTokenValidator.isValid("test-access-token")).thenReturn(true);
        when(accessTokenValidator.extractUserId("test-access-token"))
                .thenReturn("550e8400-e29b-41d4-a716-446655440000");

        mockMvc.perform(post("/api/auth/change-password")
                        .header("Authorization", "Bearer test-access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "currentPassword": "OldPassword1!",
                                    "newPassword": "NewPassword1!"
                                }
                                """))
                .andExpect(status().isNoContent())
                .andDo(document("auth/change-password",
                        requestHeaders(
                                headerWithName("Authorization").description("Bearer access token")
                        ),
                        requestFields(
                                fieldWithPath("currentPassword").description("The user's current password"),
                                fieldWithPath("newPassword").description("The new password (8 chars min, 72 bytes max; must include uppercase, lowercase, digit, and special character; must differ from the current password)")
                        )));
    }

    @Test
    void requestPasswordReset() throws Exception {
        mockMvc.perform(post("/api/auth/request-password-reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "user@example.com"}
                                """))
                .andExpect(status().isNoContent())
                .andDo(document("auth/request-password-reset",
                        requestFields(
                                fieldWithPath("email").description("The email address of the account to reset")
                        )));
    }

    @Test
    void resetPassword() throws Exception {
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "token": "abc123-reset-token",
                                    "newPassword": "NewPassword1!"
                                }
                                """))
                .andExpect(status().isNoContent())
                .andDo(document("auth/reset-password",
                        requestFields(
                                fieldWithPath("token").description("The password reset token"),
                                fieldWithPath("newPassword").description("The new password (8 chars min, 72 bytes max; must include uppercase, lowercase, digit, and special character; must differ from the current password)")
                        )));
    }

    @Test
    void getProfile() throws Exception {
        when(accessTokenValidator.isValid("test-access-token")).thenReturn(true);
        when(accessTokenValidator.extractUserId("test-access-token"))
                .thenReturn("550e8400-e29b-41d4-a716-446655440000");
        var defaultOp = new com.vointika.shared.port.UserTourOperatorMembershipsQuery.TourOperatorMembershipView(
                java.util.UUID.fromString("019dc500-0000-7000-8000-000000000001"),
                "Acme Tours",
                "https://cdn.vointika.com/operators/acme.png",
                "America/New_York",
                "USD",
                true,
                "OWNER");
        var otherOp = new com.vointika.shared.port.UserTourOperatorMembershipsQuery.TourOperatorMembershipView(
                java.util.UUID.fromString("019dc500-0000-7000-8000-000000000002"),
                "Beta Adventures",
                null,
                "Europe/London",
                "GBP",
                false,
                "STAFF");
        when(getProfileUseCase.execute(any()))
                .thenReturn(new GetProfileOutput(
                        java.util.UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
                        "John Doe",
                        "users/550e8400-e29b-41d4-a716-446655440000/0198a5c0-avatar.png",
                        "en",
                        java.util.List.of(defaultOp, otherOp)));

        mockMvc.perform(get("/api/auth/profile")
                        .header("Authorization", "Bearer test-access-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("550e8400-e29b-41d4-a716-446655440000"))
                .andExpect(jsonPath("$.context").value("users"))
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.avatarUrl").value("users/550e8400-e29b-41d4-a716-446655440000/0198a5c0-avatar.png"))
                .andExpect(jsonPath("$.language").value("en"))
                .andExpect(jsonPath("$.tourOperators[0].id").value("019dc500-0000-7000-8000-000000000001"))
                .andExpect(jsonPath("$.tourOperators[0].name").value("Acme Tours"))
                .andExpect(jsonPath("$.tourOperators[0].logoUrl").value("https://cdn.vointika.com/operators/acme.png"))
                .andExpect(jsonPath("$.tourOperators[0].timezone").value("America/New_York"))
                .andExpect(jsonPath("$.tourOperators[0].currency").value("USD"))
                .andExpect(jsonPath("$.tourOperators[0].isDefault").value(true))
                .andExpect(jsonPath("$.tourOperators[0].role").value("OWNER"))
                .andExpect(jsonPath("$.tourOperators[1].name").value("Beta Adventures"))
                .andExpect(jsonPath("$.tourOperators[1].currency").value("GBP"))
                .andExpect(jsonPath("$.tourOperators[1].isDefault").value(false))
                .andExpect(jsonPath("$.tourOperators[1].role").value("STAFF"))
                .andDo(document("auth/profile",
                        requestHeaders(
                                headerWithName("Authorization").description("Bearer access token")
                        ),
                        responseFields(
                                fieldWithPath("id").description("The user's unique identifier"),
                                fieldWithPath("context").description("The entity's collection (always 'users')"),
                                fieldWithPath("name").description("The user's display name"),
                                fieldWithPath("avatarUrl").description("Public URL of the user's avatar image (nullable; resolved at read time from the stored storage key)"),
                                fieldWithPath("language").description("The user's admin-UI language — a lowercase locale code (currently `en` or `es`)"),
                                fieldWithPath("tourOperators[]").description("Tour operators the user is a member of, ordered default-first then by name"),
                                fieldWithPath("tourOperators[].id").description("Tour operator id"),
                                fieldWithPath("tourOperators[].name").description("Tour operator display name"),
                                fieldWithPath("tourOperators[].logoUrl").description("Tour operator logo URL (omitted when null)").optional(),
                                fieldWithPath("tourOperators[].timezone").description("Tour operator's IANA timezone (e.g. \"America/New_York\")"),
                                fieldWithPath("tourOperators[].currency").description("Tour operator's ISO 4217 currency code (e.g. \"EUR\"). Non-null. The code rather than the symbol, so `Intl.NumberFormat` can derive both the symbol and the per-locale decimal count."),
                                fieldWithPath("tourOperators[].isDefault").description("True for the user's default tour operator (at most one per user)"),
                                fieldWithPath("tourOperators[].role").description("The caller's role in THIS operator: `OWNER`, `ADMIN`, or `STAFF`. Non-null (every membership has a role). Lets the admin UI gate actions per-operator without an extra fetch; the backend remains the authority (403/404).")
                        )));
    }

    @Test
    void setAvatar() throws Exception {
        when(accessTokenValidator.isValid("test-access-token")).thenReturn(true);
        when(accessTokenValidator.extractUserId("test-access-token"))
                .thenReturn("550e8400-e29b-41d4-a716-446655440000");
        when(setAvatarUseCase.execute(any()))
                .thenReturn(new SetAvatarOutput(
                        "users/550e8400-e29b-41d4-a716-446655440000/0198a5c0-avatar.png"));

        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.png", "image/png", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/auth/profile/avatar")
                        .file(file)
                        .header("Authorization", "Bearer test-access-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").value(
                        "users/550e8400-e29b-41d4-a716-446655440000/0198a5c0-avatar.png"))
                .andDo(document("auth/set-avatar",
                        requestHeaders(
                                headerWithName("Authorization").description("Bearer access token")
                        ),
                        requestParts(
                                partWithName("file").description(AVATAR_PART_DESCRIPTION)
                        ),
                        responseFields(
                                fieldWithPath("avatarUrl").description("Public URL of the new avatar (unique per upload — safe to swap in without cache busting)")
                        )));
    }

    @Test
    void changeLanguage() throws Exception {
        when(accessTokenValidator.isValid("test-access-token")).thenReturn(true);
        when(accessTokenValidator.extractUserId("test-access-token"))
                .thenReturn("550e8400-e29b-41d4-a716-446655440000");

        mockMvc.perform(post("/api/auth/profile/language")
                        .header("Authorization", "Bearer test-access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"language": "es"}
                                """))
                .andExpect(status().isNoContent())
                .andDo(document("auth/change-language",
                        requestHeaders(
                                headerWithName("Authorization").description("Bearer access token")
                        ),
                        requestFields(
                                fieldWithPath("language").description("Lowercase admin-UI locale code; must be one of the platform's supported UI languages (currently `en`, `es`). Case-insensitive input is normalized.")
                        )));
    }

    @Test
    void clearAvatar() throws Exception {
        when(accessTokenValidator.isValid("test-access-token")).thenReturn(true);
        when(accessTokenValidator.extractUserId("test-access-token"))
                .thenReturn("550e8400-e29b-41d4-a716-446655440000");

        mockMvc.perform(delete("/api/auth/profile/avatar")
                        .header("Authorization", "Bearer test-access-token"))
                .andExpect(status().isNoContent())
                .andDo(document("auth/clear-avatar",
                        requestHeaders(
                                headerWithName("Authorization").description("Bearer access token")
                        )));
    }
    /**
     * <b>Built from {@link SetAvatarUseCase}, never restated.</b> The description used
     * to hand-copy the three types and the cap; adding a type or raising the limit
     * would have left the guide advertising the old ones with a green build. Same
     * defect the media upload carried until #174, in a different context.
     */
    private static final String AVATAR_PART_DESCRIPTION =
            "The avatar image. "
            + String.join(", ", SetAvatarUseCase.allowedContentTypes().stream().sorted().toList())
            + ", at most " + SetAvatarUseCase.MAX_AVATAR_BYTES / (1024 * 1024) + " MB";

    /**
     * <b>Reuse detection is invisible in the response and drastic in effect.</b> A
     * replayed refresh token answers exactly as an unknown one does — the same 401,
     * the same message — because telling an attacker they tripped the detector would
     * defeat it. But this branch also revokes the <em>entire</em> token family, so a
     * client that retries a stale token is logged out of every session it holds, with
     * nothing in the response saying so. Published because a client cannot deduce it.
     */
    @Test
    void aReplayedRefreshTokenIs401AndEndsEverySession() throws Exception {
        when(refreshAccessTokenUseCase.execute(any()))
                .thenThrow(new UnauthorizedException("Invalid refresh token"));

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie(REFRESH_COOKIE_NAME, "already-rotated-refresh-token")))
                .andExpect(status().isUnauthorized())
                .andDo(document("auth/refresh-invalid",
                        responseFields(ApiErrorSnippets.errorFields())));
    }

    /** Wrong password and unknown account answer identically — anti-enumeration. */
    @Test
    void badCredentialsAre401() throws Exception {
        when(loginUserUseCase.execute(any()))
                .thenThrow(new UnauthorizedException("Invalid credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nobody@example.com\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized())
                .andDo(document("auth/login-invalid",
                        responseFields(ApiErrorSnippets.errorFields())));
    }

    /** The avatar allowlist, published from the real refusal. */
    @Test
    void anUnsupportedAvatarTypeIs422() throws Exception {
        when(accessTokenValidator.isValid("test-access-token")).thenReturn(true);
        when(accessTokenValidator.extractUserId("test-access-token"))
                .thenReturn("550e8400-e29b-41d4-a716-446655440000");
        when(setAvatarUseCase.execute(any()))
                .thenThrow(new InvalidFieldException(SetAvatarUseCase.unsupportedTypeMessage()));

        MockMultipartFile svg = new MockMultipartFile(
                "file", "avatar.svg", "image/svg+xml", "<svg/>".getBytes());

        mockMvc.perform(multipart("/api/auth/profile/avatar")
                        .file(svg)
                        .header("Authorization", "Bearer test-access-token"))
                .andExpect(status().isUnprocessableEntity())
                .andDo(document("auth/set-avatar-unsupported-type",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        requestParts(partWithName("file").description("A file whose content type is not on the avatar allowlist")),
                        responseFields(ApiErrorSnippets.errorFields())));
    }

}

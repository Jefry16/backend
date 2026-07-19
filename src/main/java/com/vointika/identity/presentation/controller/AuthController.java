package com.vointika.identity.presentation.controller;

import com.vointika.identity.application.dto.input.*;
import com.vointika.identity.application.usecase.*;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.media.MediaUrlResolver;
import com.vointika.shared.web.security.RefreshTokenCookieFactory;
import com.vointika.identity.presentation.request.*;
import com.vointika.identity.presentation.response.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final RegisterUserUseCase registerUserUseCase;
    private final VerifyAccountUseCase verifyAccountUseCase;
    private final ResendVerificationEmailUseCase resendVerificationEmailUseCase;
    private final LoginUserUseCase loginUserUseCase;
    private final RefreshAccessTokenUseCase refreshAccessTokenUseCase;
    private final LogoutUserUseCase logoutUserUseCase;
    private final ChangePasswordUseCase changePasswordUseCase;
    private final RequestPasswordResetUseCase requestPasswordResetUseCase;
    private final ResetPasswordUseCase resetPasswordUseCase;
    private final GetProfileUseCase getProfileUseCase;
    private final SetAvatarUseCase setAvatarUseCase;
    private final ClearAvatarUseCase clearAvatarUseCase;
    private final ChangeLanguageUseCase changeLanguageUseCase;
    private final RefreshTokenCookieFactory refreshTokenCookieFactory;
    private final MediaUrlResolver mediaUrlResolver;

    public AuthController(
            RegisterUserUseCase registerUserUseCase,
            VerifyAccountUseCase verifyAccountUseCase,
            ResendVerificationEmailUseCase resendVerificationEmailUseCase,
            LoginUserUseCase loginUserUseCase,
            RefreshAccessTokenUseCase refreshAccessTokenUseCase,
            LogoutUserUseCase logoutUserUseCase,
            ChangePasswordUseCase changePasswordUseCase,
            RequestPasswordResetUseCase requestPasswordResetUseCase,
            ResetPasswordUseCase resetPasswordUseCase,
            GetProfileUseCase getProfileUseCase,
            SetAvatarUseCase setAvatarUseCase,
            ClearAvatarUseCase clearAvatarUseCase,
            ChangeLanguageUseCase changeLanguageUseCase,
            RefreshTokenCookieFactory refreshTokenCookieFactory,
            MediaUrlResolver mediaUrlResolver
    ) {
        this.registerUserUseCase = registerUserUseCase;
        this.verifyAccountUseCase = verifyAccountUseCase;
        this.resendVerificationEmailUseCase = resendVerificationEmailUseCase;
        this.loginUserUseCase = loginUserUseCase;
        this.refreshAccessTokenUseCase = refreshAccessTokenUseCase;
        this.logoutUserUseCase = logoutUserUseCase;
        this.changePasswordUseCase = changePasswordUseCase;
        this.requestPasswordResetUseCase = requestPasswordResetUseCase;
        this.resetPasswordUseCase = resetPasswordUseCase;
        this.getProfileUseCase = getProfileUseCase;
        this.setAvatarUseCase = setAvatarUseCase;
        this.clearAvatarUseCase = clearAvatarUseCase;
        this.changeLanguageUseCase = changeLanguageUseCase;
        this.refreshTokenCookieFactory = refreshTokenCookieFactory;
        this.mediaUrlResolver = mediaUrlResolver;
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody RegisterUserRequest request) {
        registerUserUseCase.execute(new RegisterUserInput(
                request.email(),
                request.name(),
                request.password()
        ));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/verify")
    public ResponseEntity<Void> verify(@RequestParam String token) {
        verifyAccountUseCase.execute(new VerifyAccountInput(token));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<Void> resendVerification(@RequestBody ResendVerificationEmailRequest request) {
        resendVerificationEmailUseCase.execute(new ResendVerificationEmailInput(request.email()));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/login")
    public ResponseEntity<LoginUserResponse> login(@RequestBody LoginUserRequest request) {
        var output = loginUserUseCase.execute(new LoginUserInput(
                request.email(),
                request.password()
        ));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookieFactory.issue(output.refreshToken()).toString())
                .body(new LoginUserResponse(output.accessToken()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshAccessTokenResponse> refresh(
            @CookieValue(name = "${app.security.refresh-cookie.name}") String refreshToken
    ) {
        var output = refreshAccessTokenUseCase.execute(new RefreshAccessTokenInput(refreshToken));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookieFactory.issue(output.refreshToken()).toString())
                .body(new RefreshAccessTokenResponse(output.accessToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "${app.security.refresh-cookie.name}") String refreshToken,
            @AuthenticationPrincipal String userId
    ) {
        logoutUserUseCase.execute(new LogoutUserInput(userId, refreshToken));
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookieFactory.clear().toString())
                .build();
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal String userId
    ) {
        changePasswordUseCase.execute(new ChangePasswordInput(
                userId,
                request.currentPassword(),
                request.newPassword()
        ));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/request-password-reset")
    public ResponseEntity<Void> requestPasswordReset(@RequestBody RequestPasswordResetRequest request) {
        requestPasswordResetUseCase.execute(new RequestPasswordResetInput(request.email()));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@RequestBody ResetPasswordRequest request) {
        resetPasswordUseCase.execute(new ResetPasswordInput(
                request.token(),
                request.newPassword()
        ));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/profile")
    public ResponseEntity<ProfileResponse> getProfile(@AuthenticationPrincipal String userId) {
        var output = getProfileUseCase.execute(new GetProfileInput(userId));
        // The membership view already carries the resolved logo URL (resolved in
        // the query, per-operator) — pass it straight through.
        var tourOperators = output.tourOperators();
        return ResponseEntity.ok(new ProfileResponse(
                output.id(),
                output.name(),
                mediaUrlResolver.toUrl(output.avatarKey()),
                output.language(),
                tourOperators
        ));
    }

    @PostMapping(value = "/profile/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SetAvatarResponse> setAvatar(
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal String userId
    ) {
        try {
            var output = setAvatarUseCase.execute(new SetAvatarInput(
                    userId,
                    file.getContentType(),
                    file.getSize(),
                    file.getInputStream()
            ));
            return ResponseEntity.ok(new SetAvatarResponse(mediaUrlResolver.toUrl(output.avatarKey())));
        } catch (IOException e) {
            throw new InvalidFieldException("Unable to read uploaded file");
        }
    }

    @PostMapping("/profile/language")
    public ResponseEntity<Void> changeLanguage(
            @RequestBody ChangeLanguageRequest request,
            @AuthenticationPrincipal String userId
    ) {
        changeLanguageUseCase.execute(new ChangeLanguageInput(userId, request.language()));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/profile/avatar")
    public ResponseEntity<Void> clearAvatar(@AuthenticationPrincipal String userId) {
        clearAvatarUseCase.execute(new ClearAvatarInput(userId));
        return ResponseEntity.noContent().build();
    }
}

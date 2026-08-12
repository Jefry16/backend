package com.vointika.storefront.presentation.controller;

import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.storefront.application.policy.StorefrontRoutes;
import com.vointika.storefront.application.policy.TenantHandleResolver;
import com.vointika.storefront.application.port.UnlockTokenPort;
import com.vointika.storefront.application.usecase.GetPasswordPageUseCase;
import com.vointika.storefront.application.usecase.UnlockStorefrontUseCase;
import com.vointika.storefront.presentation.response.PasswordPageResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * The gate a locked storefront answers with, and the form that unlocks it.
 *
 * <p>It is the one storefront path the gate does not cover — the redirect points
 * here, so gating it would loop.
 *
 * <p><b>The submission is a form post, not a JSON body, and that is deliberate.</b>
 * Everything else the storefront serves is JSON because no theme exists to
 * render it yet; this one is different because the eventual consumer is an HTML
 * {@code <form>} on the password page, and a form posts
 * {@code application/x-www-form-urlencoded}. Deciding it now costs nothing and
 * saves changing a published contract later. The same reasoning keeps the page
 * semantics on the answers: <b>a right password redirects</b> to {@code /} with
 * the cookie set, and <b>a wrong one answers 200 with {@code error: true}</b>
 * rather than 401, because it is a page being re-shown, not an API refusing a
 * caller. Nothing remembers where the visitor was going, by decision.
 *
 * <p>No CSRF token is needed — {@code SecurityConfig} disables CSRF application
 * wide.
 *
 * <p>Not {@code StorefrontPasswordController}: {@code touroperator} already has
 * a class by that name (the admin API that <em>sets</em> the password), and two
 * classes with one simple name are one bean name — component scanning refuses
 * the context outright. Found by {@code VointikaApplicationTests}, the only test
 * that loads every context at once.
 */
@RestController
public class PasswordPageController {

    private final TenantHandleResolver tenantHandleResolver;
    private final GetPasswordPageUseCase getPasswordPageUseCase;
    private final UnlockStorefrontUseCase unlockStorefrontUseCase;

    public PasswordPageController(TenantHandleResolver tenantHandleResolver,
                                  GetPasswordPageUseCase getPasswordPageUseCase,
                                  UnlockStorefrontUseCase unlockStorefrontUseCase) {
        this.tenantHandleResolver = tenantHandleResolver;
        this.getPasswordPageUseCase = getPasswordPageUseCase;
        this.unlockStorefrontUseCase = unlockStorefrontUseCase;
    }

    @GetMapping(path = StorefrontRoutes.PASSWORD, produces = MediaType.APPLICATION_JSON_VALUE)
    public PasswordPageResponse form(HttpServletRequest request) {
        return page(request, false);
    }

    @PostMapping(path = StorefrontRoutes.PASSWORD, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PasswordPageResponse> unlock(
            @RequestParam(name = "password", required = false) String password,
            HttpServletRequest request) {

        String handle = tenantHandleResolver.resolve(request.getServerName())
                .orElseThrow(PasswordPageController::notFound);

        return unlockStorefrontUseCase.execute(handle, password)
                .map(token -> ResponseEntity.status(HttpStatus.FOUND)
                        .header(HttpHeaders.SET_COOKIE, unlockCookie(token, request).toString())
                        .header(HttpHeaders.LOCATION, StorefrontRoutes.HOME)
                        .<PasswordPageResponse>build())
                .orElseGet(() -> ResponseEntity.ok(page(request, true)));
    }

    /**
     * A session cookie, so closing the browser re-locks the store — the operator
     * handed out a password, not a licence. {@code Secure} follows the request
     * rather than a config flag, so dev over plain HTTP works and production over
     * TLS is marked; the cookie is host-scoped either way, so one tenant's unlock
     * never reaches another's.
     */
    private static ResponseCookie unlockCookie(String token, HttpServletRequest request) {
        return ResponseCookie.from(UnlockTokenPort.COOKIE_NAME, token)
                .httpOnly(true)
                .secure(request.isSecure())
                .sameSite("Lax")
                .path("/")
                .build();
    }

    private PasswordPageResponse page(HttpServletRequest request, boolean error) {
        return tenantHandleResolver.resolve(request.getServerName())
                .flatMap(getPasswordPageUseCase::execute)
                .map(output -> PasswordPageResponse.from(output, error))
                .orElseThrow(PasswordPageController::notFound);
    }

    private static ResourceNotFoundException notFound() {
        return new ResourceNotFoundException("There is no storefront at this address");
    }
}

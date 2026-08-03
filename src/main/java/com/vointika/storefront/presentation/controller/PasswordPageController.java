package com.vointika.storefront.presentation.controller;

import com.samskivert.mustache.Template;
import com.vointika.storefront.application.dto.output.PasswordPageOutput;
import com.vointika.storefront.application.policy.TenantHandleResolver;
import com.vointika.storefront.application.port.UnlockTokenPort;
import com.vointika.storefront.application.usecase.GetPasswordPageUseCase;
import com.vointika.storefront.application.usecase.UnlockStorefrontUseCase;
import com.vointika.storefront.presentation.view.PasswordView;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

/**
 * The page a locked storefront answers with, and the form that unlocks it.
 *
 * <p>It is the one storefront path the gate does not cover — the redirect points
 * here, so gating it would loop. Everything it shows is public by construction:
 * the shop's name and the operator's own message for visitors.
 *
 * <p><b>Rendered in the primary locale only.</b> The gate runs before locale
 * resolution, so there is no path locale to render in, and reading one would
 * reintroduce exactly the leak the ordering prevents.
 *
 * <p>A wrong password re-renders at <b>200</b> and sets no cookie; a right one
 * sets the unlock cookie and redirects to {@code /}. Nothing remembers where the
 * visitor was going, by decision.
 *
 * <p>The form needs no CSRF token — {@code SecurityConfig} disables CSRF for the
 * whole application.
 *
 * <p>Not {@code StorefrontPasswordController}: {@code touroperator} already has
 * a class by that name (the admin API that <em>sets</em> the password), and two
 * classes with one simple name are one bean name — component scanning refuses
 * the context outright. Found by {@code VointikaApplicationTests}, which is the
 * only test that loads every context at once.
 */
@Controller
public class PasswordPageController {

    private static final MediaType HTML_UTF8 = new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8);

    private final TenantHandleResolver tenantHandleResolver;
    private final GetPasswordPageUseCase getPasswordPageUseCase;
    private final UnlockStorefrontUseCase unlockStorefrontUseCase;
    private final Template passwordTemplate;
    private final Template notFoundTemplate;

    public PasswordPageController(TenantHandleResolver tenantHandleResolver,
                                        GetPasswordPageUseCase getPasswordPageUseCase,
                                        UnlockStorefrontUseCase unlockStorefrontUseCase,
                                        @Qualifier("storefrontPasswordTemplate") Template passwordTemplate,
                                        @Qualifier("storefrontNotFoundTemplate") Template notFoundTemplate) {
        this.tenantHandleResolver = tenantHandleResolver;
        this.getPasswordPageUseCase = getPasswordPageUseCase;
        this.unlockStorefrontUseCase = unlockStorefrontUseCase;
        this.passwordTemplate = passwordTemplate;
        this.notFoundTemplate = notFoundTemplate;
    }

    @GetMapping(path = "/password", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> form(HttpServletRequest request) {
        return page(request, false);
    }

    @PostMapping(path = "/password", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> unlock(@RequestParam(name = "password", required = false) String password,
                                         HttpServletRequest request) {
        Optional<String> handle = tenantHandleResolver.resolve(request.getServerName());
        if (handle.isEmpty()) {
            return notFound();
        }
        Optional<String> token = unlockStorefrontUseCase.execute(handle.get(), password);
        if (token.isEmpty()) {
            return page(request, true);
        }
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.SET_COOKIE, unlockCookie(token.get(), request).toString())
                .header(HttpHeaders.LOCATION, "/")
                .build();
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

    private ResponseEntity<String> page(HttpServletRequest request, boolean error) {
        Optional<PasswordPageOutput> page = tenantHandleResolver.resolve(request.getServerName())
                .flatMap(getPasswordPageUseCase::execute);
        return page
                .map(content -> ResponseEntity.ok()
                        .contentType(HTML_UTF8)
                        .body(passwordTemplate.execute(PasswordView.from(content, error))))
                .orElseGet(this::notFound);
    }

    private ResponseEntity<String> notFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(HTML_UTF8)
                .body(notFoundTemplate.execute(Map.of()));
    }
}

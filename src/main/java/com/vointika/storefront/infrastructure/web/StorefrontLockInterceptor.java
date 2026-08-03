package com.vointika.storefront.infrastructure.web;

import com.vointika.storefront.application.policy.TenantHandleResolver;
import com.vointika.storefront.application.port.UnlockTokenPort;
import com.vointika.storefront.application.usecase.CheckStorefrontLockUseCase;
import com.vointika.storefront.application.usecase.CheckStorefrontLockUseCase.LockState;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.Optional;

/**
 * The password gate: on a locked storefront every path answers {@code 302} to
 * {@code /password}, and nothing downstream runs.
 *
 * <p><b>An interceptor rather than a check in each controller.</b> The rule is
 * "every storefront path"; a controller-side check is one new route away from
 * being forgotten, and forgetting it is a leak rather than a bug. Mirrors the
 * admin membership interceptor.
 *
 * <p>The redirect carries no navigation and no return address — a locked store
 * answers a request for a locale it does not publish exactly as it answers
 * {@code /}, so nothing about the store leaks from in front of the gate.
 *
 * <p>Both collaborators are {@link ObjectProvider}s, resolved per request, so
 * this constructs inside any {@code @WebMvcTest} slice that does not supply
 * them: {@code @WebMvcTest} pulls in every {@code WebMvcConfigurer}, and the
 * only requests that reach the resolution are the storefront's own.
 */
public class StorefrontLockInterceptor implements HandlerInterceptor {

    private final ObjectProvider<TenantHandleResolver> tenantHandleResolver;
    private final ObjectProvider<CheckStorefrontLockUseCase> checkStorefrontLock;

    public StorefrontLockInterceptor(ObjectProvider<TenantHandleResolver> tenantHandleResolver,
                                     ObjectProvider<CheckStorefrontLockUseCase> checkStorefrontLock) {
        this.tenantHandleResolver = tenantHandleResolver;
        this.checkStorefrontLock = checkStorefrontLock;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Optional<String> handle = tenantHandleResolver.getObject().resolve(request.getServerName());
        if (handle.isEmpty()) {
            return true;
        }
        LockState state = checkStorefrontLock.getObject().execute(handle.get(), unlockCookie(request));
        if (state != LockState.LOCKED) {
            // An unknown tenant passes through so the controller answers it with
            // the not-found page, in one place.
            return true;
        }
        response.setStatus(HttpStatus.FOUND.value());
        response.setHeader(HttpHeaders.LOCATION, "/password");
        return false;
    }

    private static String unlockCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        return Arrays.stream(cookies)
                .filter(cookie -> UnlockTokenPort.COOKIE_NAME.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}

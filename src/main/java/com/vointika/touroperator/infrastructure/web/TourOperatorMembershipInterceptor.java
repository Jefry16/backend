package com.vointika.touroperator.infrastructure.web;

import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Gates every {@code /api/tour-operators/{id}/**} request on membership: the
 * authenticated caller must be a member of the operator in the path, else 404
 * (identical to a missing operator — tenant isolation). Role checks live in the
 * use cases; this only enforces membership.
 */
public class TourOperatorMembershipInterceptor implements HandlerInterceptor {

    // Grab the first path segment after the base and parse it with the SAME
    // UUID.fromString the @PathVariable binder uses — the gate must never be
    // narrower than the binder (a tighter char-class regex once let lenient
    // UUID forms bind to the real operator while slipping past the gate — IDOR).
    private static final Pattern OPERATOR_ID_PATTERN =
            Pattern.compile("^/api/tour-operators/([^/]+)(?:/.*)?$");

    private final ObjectProvider<TourOperatorMembershipCheck> membershipCheck;

    public TourOperatorMembershipInterceptor(ObjectProvider<TourOperatorMembershipCheck> membershipCheck) {
        this.membershipCheck = membershipCheck;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Matcher matcher = OPERATOR_ID_PATTERN.matcher(request.getRequestURI());
        if (!matcher.matches()) {
            return true;
        }

        UUID tourOperatorId = parseUuid(matcher.group(1));
        UUID userId = principalUserId();
        membershipCheck.getObject().ensureMember(userId, tourOperatorId);
        return true;
    }

    /** The operator id out of the URI. A malformed one is a 404, like a missing operator. */
    private static UUID parseUuid(String raw) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            throw new ResourceNotFoundException(TourOperatorMembershipCheck.TENANT_NOT_FOUND);
        }
    }

    /**
     * A non-UUID principal is the anonymous one, and it gets the same 404 every
     * non-member gets. It no longer round-trips through text: {@code
     * JwtAuthenticationFilter} stores the parsed UUID, so this reads it rather
     * than re-parsing {@code toString()}.
     */
    private static UUID principalUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UUID userId)) {
            throw new ResourceNotFoundException(TourOperatorMembershipCheck.TENANT_NOT_FOUND);
        }
        return userId;
    }
}

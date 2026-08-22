package com.vointika.storefront.infrastructure.security;

import com.vointika.storefront.application.policy.StorefrontNotFound;
import com.vointika.storefront.application.policy.TenantHandleResolver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Who the storefront claims when Spring Security has already refused a request.
 *
 * <p>The claim is deliberately coarse — a tenant host owns every path on it — so
 * what these pin is the <b>boundary</b>: the two cases that must stay a 401, and
 * the case that must not fail a test slice.
 */
class StorefrontUnauthenticatedRequestsTest {

    private static final String BASE_DOMAIN = "localhost";

    private static StorefrontUnauthenticatedRequests withResolver() {
        return new StorefrontUnauthenticatedRequests(
                provider(new TenantHandleResolver(BASE_DOMAIN)));
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<TenantHandleResolver> provider(TenantHandleResolver resolver) {
        ObjectProvider<TenantHandleResolver> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(resolver);
        return provider;
    }

    private static MockHttpServletRequest request(String host, String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        request.setServerName(host);
        return request;
    }

    @Test
    void aTenantHostClaimsAPathThatMatchesNoRoute() {
        Optional<String> message = withResolver()
                .notFoundMessage(request("acme.localhost", "/policies/legal_notice"));

        assertThat(message).contains(StorefrontNotFound.MESSAGE);
    }

    /**
     * The claim is the host, not a list of namespaces. A path under no storefront
     * namespace at all is still the storefront's on the storefront's host — which
     * is the property that keeps this from becoming the route allowlist written
     * twice.
     */
    @Test
    void aTenantHostClaimsAPathUnderNoStorefrontNamespace() {
        assertThat(withResolver().notFoundMessage(request("acme.localhost", "/wp-admin")))
                .contains(StorefrontNotFound.MESSAGE);
    }

    /**
     * <b>The admin API keeps its 401 even on a tenant host.</b> Not because the API
     * is served there — it is not, in any deployment we intend — but because the
     * dev stack puts both surfaces on one origin, and an expired token answering
     * "There is no storefront at this address" sends the reader hunting a routing
     * bug that does not exist.
     */
    @Test
    void theAdminApiKeepsItsUnauthorizedEvenOnATenantHost() {
        assertThat(withResolver().notFoundMessage(
                request("acme.localhost", "/api/tour-operators/019f7f33-1833-7dc1-b008-47e6c68b3ea2")))
                .isEmpty();
    }

    /** The apex addresses no tenant, so nothing there is claimed. */
    @Test
    void theApexHostIsNotClaimed() {
        assertThat(withResolver().notFoundMessage(request("localhost", "/pages/About_Us"))).isEmpty();
    }

    /**
     * A reserved label is infrastructure, never a tenant — the resolver's own rule,
     * and this proves the policy inherits it rather than reimplementing a host
     * check of its own.
     */
    @Test
    void aReservedLabelIsNotATenant() {
        assertThat(withResolver().notFoundMessage(request("api.localhost", "/pages/About_Us"))).isEmpty();
    }

    /**
     * A {@code @WebMvcTest} slice loads {@code SecurityConfig} without the
     * storefront's own beans. No resolver must mean no claim, not a failed
     * context — every other context's slice tests depend on the 401 staying a 401.
     */
    @Test
    void noResolverMeansNoClaim() {
        StorefrontUnauthenticatedRequests policy =
                new StorefrontUnauthenticatedRequests(provider(null));

        assertThat(policy.notFoundMessage(request("acme.localhost", "/pages/About_Us"))).isEmpty();
    }
}

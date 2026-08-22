package com.vointika.shared.web.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The body written when Spring Security refuses a request before MVC exists.
 *
 * <p>Hand-written JSON, so it is worth a test that actually parses it: there are
 * no message converters this early in the chain, and a body no client can read is
 * a failure mode nothing else in the application has.
 */
class RestAuthenticationEntryPointTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static JsonNode commence(List<UnauthenticatedRequestPolicy> policies) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        new RestAuthenticationEntryPoint(policies).commence(
                new MockHttpServletRequest("GET", "/anything"),
                response,
                new InsufficientAuthenticationException("no token"));
        assertThat(response.getContentType()).isEqualTo("application/json");
        return MAPPER.readTree(response.getContentAsString());
    }

    private static UnauthenticatedRequestPolicy claiming(String message) {
        return request -> Optional.of(message);
    }

    private static UnauthenticatedRequestPolicy claimingNothing() {
        return request -> Optional.empty();
    }

    @Test
    void withNoPolicyItIsTheUnchangedUnauthorizedBody() throws Exception {
        JsonNode body = commence(List.of());

        assertThat(body.get("status").asInt()).isEqualTo(401);
        assertThat(body.get("error").asText()).isEqualTo("Unauthorized");
        assertThat(body.get("message").asText()).isEqualTo("Authentication required");
        assertThat(body.get("timestamp").asText()).isNotBlank();
    }

    /** A policy that declines leaves the 401 exactly as it was. */
    @Test
    void aPolicyThatDeclinesChangesNothing() throws Exception {
        assertThat(commence(List.of(claimingNothing())).get("status").asInt()).isEqualTo(401);
    }

    @Test
    void aClaimBecomesA404CarryingTheContextsOwnMessage() throws Exception {
        JsonNode body = commence(List.of(claiming("There is no storefront at this address")));

        assertThat(body.get("status").asInt()).isEqualTo(404);
        assertThat(body.get("error").asText()).isEqualTo("Not Found");
        assertThat(body.get("message").asText()).isEqualTo("There is no storefront at this address");
    }

    /** First claim wins, and a declining policy ahead of it does not swallow it. */
    @Test
    void theFirstClaimWins() throws Exception {
        JsonNode body = commence(List.of(claimingNothing(), claiming("mine"), claiming("not mine")));

        assertThat(body.get("message").asText()).isEqualTo("mine");
    }

    /**
     * <b>The message crosses a context boundary into hand-built JSON.</b> No
     * message today contains a quote, so this is guarding the mechanism rather
     * than a live case — but the failure it prevents is a body that no client can
     * parse, produced by an edit in a different package that looks harmless.
     */
    @Test
    void aQuoteInTheMessageDoesNotBreakTheBody() throws Exception {
        JsonNode body = commence(List.of(claiming("a \"quoted\" thing with a \\ backslash")));

        assertThat(body.get("message").asText()).isEqualTo("a \"quoted\" thing with a \\ backslash");
    }
}

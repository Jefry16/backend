package com.vointika.storefront.application.policy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TenantHandleResolverTest {

    private final TenantHandleResolver resolver =
            new TenantHandleResolver("localhost");

    @Test
    void resolvesTheLabelInFrontOfTheBaseDomain() {
        assertThat(resolver.resolve("acme.localhost")).contains("acme");
    }

    @Test
    void stripsThePort() {
        assertThat(resolver.resolve("acme.localhost:8080")).contains("acme");
    }

    @Test
    void foldsCaseWithLocaleRoot() {
        assertThat(resolver.resolve("ACME.LOCALHOST")).contains("acme");
    }

    @Test
    void rejectsTheApex() {
        assertThat(resolver.resolve("localhost:8080")).isEmpty();
    }

    /**
     * The second lock on reserved labels. Creation refuses to mint one, so this
     * only fires for a row that predates the rule or was written straight into
     * the database — but that is exactly the case where {@code api.vointika.com}
     * would otherwise serve a tenant instead of the API.
     */
    @Test
    void rejectsAReservedLabelEvenThoughItIsWellFormed() {
        assertThat(resolver.resolve("api.localhost")).isEmpty();
        assertThat(resolver.resolve("admin.localhost:8080")).isEmpty();
        assertThat(resolver.resolve("API.localhost")).isEmpty();
        // and does not over-reject a name that merely starts the same way
        assertThat(resolver.resolve("apidae.localhost")).contains("apidae");
    }

    /** A crafted multi-label host must not be read as the tenant it prefixes. */
    @Test
    void rejectsAMultiLabelHost() {
        assertThat(resolver.resolve("a.b.localhost")).isEmpty();
    }

    @Test
    void rejectsAMissingOrBlankHost() {
        assertThat(resolver.resolve(null)).isEmpty();
        assertThat(resolver.resolve("   ")).isEmpty();
    }

    @Test
    void rejectsAHostOnAnotherDomain() {
        assertThat(resolver.resolve("acme.example.com")).isEmpty();
    }
}

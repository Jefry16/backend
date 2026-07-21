package com.vointika.shared.infrastructure.flyway;

import org.flywaydb.core.Flyway;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.List;

/**
 * Runs Flyway once per bounded context, each with its own history table
 * ({@code <domain>.flyway_schema_history}, isolated by schema). This lets every context
 * own an independent version sequence ({@code V1__}, {@code V2__}, …)
 * and ship migrations without coordinating global version numbers.
 *
 * <p>Replaces the default Spring Boot Flyway migrate strategy. The
 * autoconfigured {@code Flyway} bean still exists but is never migrated;
 * per-domain instances are created and migrated here instead.
 *
 * <p>To add a new bounded context's migrations: create
 * {@code db/migration/<domain>/V1__....sql} and add {@code <domain>} to
 * {@link #DOMAINS}.
 */
@Configuration
public class FlywayPerDomainConfig {

    // Order is dependency order, NOT alphabetical. Each domain's migrations
    // may reference tables in earlier-listed schemas via cross-schema foreign
    // keys. Adding a new domain: append it after every domain whose schema it
    // FKs into. (notification owns no tables, so it is absent here.)
    private static final List<String> DOMAINS = List.of(
            "identity",
            // reference is self-contained (no cross-schema FKs), so its position
            // relative to identity is unconstrained.
            "reference",
            // touroperator FKs into identity.users + reference.timezones/currencies,
            // so it must come after both.
            "touroperator",
            // media FKs into touroperator.tour_operators + identity.users, so it
            // must come after both.
            "media",
            // experience FKs into touroperator.tour_operators + identity.users
            // (media ids are bare, no FK), so it must come after both.
            "experience"
    );

    @Bean
    public FlywayMigrationStrategy perDomainFlywayMigrationStrategy(DataSource dataSource) {
        return defaultFlyway -> DOMAINS.forEach(domain ->
                Flyway.configure()
                        .dataSource(dataSource)
                        .locations("classpath:db/migration/" + domain)
                        .schemas(domain)
                        .defaultSchema(domain)
                        .createSchemas(true)
                        .placeholderReplacement(false)
                        .load()
                        .migrate());
    }
}

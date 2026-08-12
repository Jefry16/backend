package com.vointika.touroperator.infrastructure.persistence;

import com.vointika.touroperator.domain.enums.BrandColorRole;
import com.vointika.touroperator.domain.enums.BrandSocialPlatform;
import com.vointika.architecture.MigrationCheckConstraints;
import org.junit.jupiter.api.Test;

import java.io.IOException;

/**
 * {@link BrandSocialPlatform} and {@link BrandColorRole} each pair with a CHECK
 * constraint, and nothing but this makes them agree.
 *
 * <p>This project has already shipped that drift once — {@code AuditActorType}
 * against {@code audit_log_actor_type_check} — and an audit caught it rather
 * than the build. {@link MigrationCheckConstraints#assertEnumMatches} carries
 * what the failure costs in each direction;
 * {@code PolicyTypeMatchesTheCheckConstraintTest} is the same shape over the
 * same helper.
 */
class BrandEnumsMatchTheCheckConstraintsTest {

    @Test
    void everySocialPlatformIsAcceptedByTheCheckConstraint() throws IOException {
        MigrationCheckConstraints.assertEnumMatches("touroperator", "platform", BrandSocialPlatform.class,
                "tour_operator_brand_social_links");
    }

    @Test
    void everyColorRoleIsAcceptedByTheCheckConstraint() throws IOException {
        MigrationCheckConstraints.assertEnumMatches("touroperator", "role", BrandColorRole.class,
                "tour_operator_brand_colors");
    }
}

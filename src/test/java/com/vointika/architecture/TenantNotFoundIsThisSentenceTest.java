package com.vointika.architecture;

import com.vointika.shared.port.TourOperatorMembershipCheck;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The one place allowed to write the tenant-isolation 404 out, so that its wording
 * is pinned somewhere.
 *
 * <p><b>Why this exists.</b> Collapsing sixteen test copies onto the constant made
 * every assertion tautological — production throws {@code TENANT_NOT_FOUND} and the
 * test asserted {@code hasMessage(TENANT_NOT_FOUND)}, which holds for any value. And
 * {@link TenantNotFoundMessageIsWrittenOnceTest} then forecloses the obvious fix: it
 * scans {@code src/test} too, so re-pinning the sentence in an ordinary assertion
 * fails the build.
 *
 * <p>That combination left the sentence <b>unassertable</b> — a strictly weaker
 * position than the sixteen copies it replaced. Caught in review by rewording the
 * constant to "Operator missing": the suite stayed green while <b>eight published
 * snippets</b> changed the 404 body of every tenant-scoped endpoint in the API guide.
 *
 * <p>So this file is exempted by name in that guard, exactly as the constant's own
 * declaration is. It is the only assertion in the repository that may spell the
 * sentence, and it must stay a single line: the moment it grows, "written once"
 * stops being true in the way the other guard promises.
 */
class TenantNotFoundIsThisSentenceTest {

    @Test
    void theTenantIsolation404ReadsThisWay() {
        assertThat(TourOperatorMembershipCheck.TENANT_NOT_FOUND)
                .withFailMessage("""
                        The tenant-isolation 404 changed wording. That is a published \
                        contract: it is the message body of every tenant-scoped \
                        endpoint's 404 in the API guide, and eight snippets move with \
                        it. If the change is intended, update this line — it is the \
                        only place in the repository allowed to say the sentence, and \
                        the point of it is that the change cannot happen silently.""")
                .isEqualTo("Tour operator not found");
    }
}

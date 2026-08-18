package com.vointika.shared.valueobject;

import com.vointika.shared.exception.InvalidFieldException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The refusals of {@link SeoTitle} and {@link SeoDescription}, which three contexts
 * now share.
 *
 * <p><b>Nothing pinned these before — not here and not in the six per-context records
 * this replaced.</b> Those six were constructed in tests only on accept paths, so no
 * assertion anywhere exercised a blank, an over-length or a control character. The
 * collapse was therefore faithful (the six were byte-identical bar javadoc) and
 * completely unguarded, which is worse now that one edit reaches every context
 * instead of one.
 *
 * <p>{@link #aLeadingNewlineIsRefusedRatherThanTrimmedAway} is the load-bearing one.
 * {@code SeoTextRules} scans for control characters <em>before</em> trimming, and its
 * javadoc calls that ordering deliberate — but swapping the two lines kept the whole
 * suite green while silently accepting {@code "\nHello"}. Swap them and watch only
 * that test fail.
 */
class SeoTextValueObjectsTest {

    @Test
    void aBlankTitleIsRefused() {
        assertThatThrownBy(() -> new SeoTitle("   "))
                .isInstanceOf(InvalidFieldException.class)
                .hasMessage("SEO title cannot be blank");
        assertThatThrownBy(() -> new SeoTitle(null))
                .isInstanceOf(InvalidFieldException.class);
    }

    @Test
    void aBlankDescriptionIsRefused() {
        assertThatThrownBy(() -> new SeoDescription("   "))
                .isInstanceOf(InvalidFieldException.class)
                .hasMessage("SEO description cannot be blank");
    }

    /**
     * The ordering pin. {@code String.trim} strips everything at or below U+0020, so
     * trimming first would delete the very character the scan exists to reject.
     */
    @Test
    void aLeadingNewlineIsRefusedRatherThanTrimmedAway() {
        assertThatThrownBy(() -> new SeoTitle("\nHello"))
                .isInstanceOf(InvalidFieldException.class)
                .hasMessage("SEO title contains an invalid character");
        assertThatThrownBy(() -> new SeoDescription("Hello\tthere"))
                .isInstanceOf(InvalidFieldException.class)
                .hasMessage("SEO description contains an invalid character");
    }

    @Test
    void anEmbeddedFormatCharacterIsRefused() {
        assertThatThrownBy(() -> new SeoTitle("He​llo"))
                .isInstanceOf(InvalidFieldException.class)
                .hasMessage("SEO title contains an invalid character");
    }

    @Test
    void theLimitsAreSeventyAndThreeHundredAndTwenty() {
        assertThat(new SeoTitle("a".repeat(SeoTitle.MAX_LENGTH)).value())
                .hasSize(SeoTitle.MAX_LENGTH);
        assertThatThrownBy(() -> new SeoTitle("a".repeat(SeoTitle.MAX_LENGTH + 1)))
                .isInstanceOf(InvalidFieldException.class)
                .hasMessage("SEO title must be between 1 and 70 characters");

        assertThat(new SeoDescription("a".repeat(SeoDescription.MAX_LENGTH)).value())
                .hasSize(SeoDescription.MAX_LENGTH);
        assertThatThrownBy(() -> new SeoDescription("a".repeat(SeoDescription.MAX_LENGTH + 1)))
                .isInstanceOf(InvalidFieldException.class)
                .hasMessage("SEO description must be between 1 and 320 characters");
    }

    /**
     * The length check runs on the trimmed value, so padding does not consume budget.
     * This is the half of the ordering that trimming-first would <em>not</em> have
     * broken, which is why the newline case above is the one that matters.
     */
    @Test
    void surroundingWhitespaceIsTrimmedAndDoesNotCountTowardTheLimit() {
        assertThat(new SeoTitle("  Hello  ").value()).isEqualTo("Hello");
        assertThat(new SeoTitle(" " + "a".repeat(SeoTitle.MAX_LENGTH) + " ").value())
                .hasSize(SeoTitle.MAX_LENGTH);
    }
}

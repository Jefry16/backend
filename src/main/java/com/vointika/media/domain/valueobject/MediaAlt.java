package com.vointika.media.domain.valueobject;

import com.vointika.shared.exception.InvalidFieldException;

/**
 * The alternative text for an image — what a screen reader announces and what a
 * search engine reads.
 *
 * <p><b>It cannot be derived, which is why it needs a write path of its own.</b>
 * Width and height are measurable from the file; alt is a description only the
 * person who chose the image can supply, so it arrives after the upload rather
 * than during it.
 *
 * <p>500 characters, matching {@code media.alt}. Absence is modelled outside
 * this type: an image with no alt holds null, not an empty string — the two mean
 * different things to a screen reader, where {@code alt=""} declares an image
 * decorative.
 */
public record MediaAlt(String value) {

    public static final int MAX_LENGTH = 500;

    public MediaAlt {
        if (value == null || value.isBlank()) {
            throw new InvalidFieldException("Alt text cannot be blank");
        }
        for (int i = 0, len = value.length(); i < len; i++) {
            int type = Character.getType(value.charAt(i));
            if (type == Character.CONTROL || type == Character.FORMAT) {
                throw new InvalidFieldException("Alt text contains an invalid character");
            }
        }
        value = value.trim();
        if (value.isEmpty() || value.length() > MAX_LENGTH) {
            throw new InvalidFieldException(
                    "Alt text must be between 1 and " + MAX_LENGTH + " characters");
        }
    }
}

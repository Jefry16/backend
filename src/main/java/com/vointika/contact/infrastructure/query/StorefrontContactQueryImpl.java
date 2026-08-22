package com.vointika.contact.infrastructure.query;

import com.vointika.contact.domain.valueobject.ContactContent;
import com.vointika.contact.domain.valueobject.ContactEmail;
import com.vointika.contact.domain.valueobject.ContactName;
import com.vointika.contact.domain.valueobject.ContactSummary;
import com.vointika.shared.port.StorefrontContactQuery;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * The contact form's shape, read off the domain rather than described again.
 *
 * <p>Every limit here is the value object's own {@code MAX_LENGTH}, so the form a
 * visitor sees and the validation that would reject them cannot disagree. That is
 * the whole reason this is an adapter in {@code contact} and not a constant in
 * {@code storefront}.
 *
 * <p><b>All four are required, and that is stated per field rather than once.</b>
 * Each value object throws its own "X is required", so required-ness is a fact
 * about a field; it being uniformly true today is not the same as it being a
 * property of the form. A theme reading a single form-level flag would be wrong
 * the day one field becomes optional, and nothing would fail.
 *
 * <p>It touches no database. There is nothing tenant-specific about what a
 * contact message is.
 */
@Component
public class StorefrontContactQueryImpl implements StorefrontContactQuery {

    private static final ContactFormView FORM = new ContactFormView(List.of(
            new FieldView("name", true, ContactName.MAX_LENGTH),
            new FieldView("email", true, ContactEmail.MAX_LENGTH),
            new FieldView("summary", true, ContactSummary.MAX_LENGTH),
            new FieldView("content", true, ContactContent.MAX_LENGTH)));

    @Override
    public ContactFormView form() {
        return FORM;
    }
}

package com.vointika.storefront.application.dto.output;

import com.vointika.shared.port.StorefrontContactQuery.ContactFormView;

/**
 * The globals plus the form, the same pairing every other page makes with its own
 * object.
 *
 * <p>The form is not tenant data and does not vary by locale, so unlike
 * {@code page} or {@code experience} it needs no lookup that can miss. The page
 * therefore has only one 404: the operator or the locale, decided by the globals.
 */
public record StorefrontContactOutput(StorefrontGlobals globals, ContactFormView form) {}

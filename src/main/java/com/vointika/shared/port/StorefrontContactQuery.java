package com.vointika.shared.port;

import java.util.List;

/**
 * What the contact inbox accepts, for the storefront's contact page. Implemented
 * in {@code contact}.
 *
 * <p><b>It answers a static question, and it is still a port.</b> Nothing here
 * varies by tenant or locale — the shape of a contact message is the same for
 * every operator. The seam exists because the rule belongs to {@code contact} and
 * {@code storefront} may not import it (ArchUnit): the alternative is the four
 * field names and their limits retyped in the storefront, which is a copy that
 * drifts silently the first time a limit moves.
 *
 * <p><b>This is the first reader of the parked contact domain.</b>
 * {@code ContactMessage.submit} and the four value objects were kept when intake
 * was deleted, on the argument that the domain is what makes the intake cheap to
 * bring back. The limits below are read off those value objects, so the page and
 * the validation cannot disagree even now, while nothing validates.
 *
 * <p><b>There is no {@code action}.</b> A form posts somewhere, and that
 * somewhere does not exist — intake is deleted. Publishing a URL nothing serves
 * would be the one kind of wrong a contract must not be, and adding the field
 * later is additive rather than breaking.
 */
public interface StorefrontContactQuery {

    ContactFormView form();

    /**
     * @param fields in the order a form should render them, which is the order
     *               the inbox reads: who, how to reach them, what about, then the
     *               message. Order is data here — a list is an ordered thing and a
     *               theme should not have to re-derive a sensible sequence.
     */
    record ContactFormView(List<FieldView> fields) {}

    /**
     * @param name  the wire name, matching the column and the admin API
     *              ({@code summary}, not {@code subject}) so one message has one
     *              set of field names everywhere. A visitor-facing <em>label</em>
     *              is a theme concern and is deliberately absent: it has to be
     *              translated, and the theme is what holds translations.
     * @param maxLength read from the domain value object, never retyped.
     */
    record FieldView(String name, boolean required, int maxLength) {}
}

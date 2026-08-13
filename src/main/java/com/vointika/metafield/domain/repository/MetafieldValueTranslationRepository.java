package com.vointika.metafield.domain.repository;

import com.vointika.metafield.domain.entity.MetafieldValueTranslation;
import com.vointika.metafield.domain.valueobject.MetafieldOwnerType;

import java.util.List;
import java.util.UUID;

/**
 * Reads and writes are both addressed by the OWNER, never by a value id the
 * caller had to look up first: the admin edits "this page's translations for
 * fr", and the storefront reads "this operator's values in en".
 */
public interface MetafieldValueTranslationRepository {

    void upsert(MetafieldValueTranslation translation);

    /** @return true when a row was actually removed, so the caller can skip auditing a no-op */
    boolean delete(UUID metafieldValueId, String locale);

    /** @return how many rows went, for the same reason */
    int deleteForOwner(UUID tourOperatorId, MetafieldOwnerType ownerType, UUID ownerId, String locale);

    /** One locale's overlay for one owner, as {@code namespace.key → value}. */
    List<TranslatedValue> findForOwner(
            UUID tourOperatorId, MetafieldOwnerType ownerType, UUID ownerId, String locale);

    /** Which locales this owner has any translation in, ascending — the editor's switcher. */
    List<String> findLocalesForOwner(
            UUID tourOperatorId, MetafieldOwnerType ownerType, UUID ownerId);

    record TranslatedValue(String namespace, String key, String value) {}
}

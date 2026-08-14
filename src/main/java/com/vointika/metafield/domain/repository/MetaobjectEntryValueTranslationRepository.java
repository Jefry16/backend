package com.vointika.metafield.domain.repository;

import com.vointika.metafield.domain.entity.MetaobjectEntryValueTranslation;

import java.util.List;
import java.util.UUID;

/**
 * Addressed by the ENTRY, never by a value id the caller had to look up — the
 * admin edits "this boat's fields in fr".
 */
public interface MetaobjectEntryValueTranslationRepository {

    void upsert(MetaobjectEntryValueTranslation translation);

    /** @return true when a row actually went, so a no-op is not audited */
    boolean delete(UUID entryValueId, String locale);

    /** @return how many rows went, for the same reason */
    int deleteForEntry(UUID entryId, String locale);

    /** One locale's overlay for one entry, as {@code fieldKey → value}. */
    List<TranslatedField> findForEntry(UUID entryId, String locale);

    /** Which locales this entry has any translation in, ascending. */
    List<String> findLocalesForEntry(UUID entryId);

    record TranslatedField(String fieldKey, String value) {}
}

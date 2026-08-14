package com.vointika.metafield.infrastructure.query;

import com.vointika.metafield.domain.projection.MetafieldValueWithDefinition;
import com.vointika.metafield.domain.projection.PublishedMetaobjectField;
import com.vointika.metafield.domain.repository.MetafieldValueRepository;
import com.vointika.metafield.domain.valueobject.MetafieldOwnerType;
import com.vointika.metafield.domain.valueobject.MetafieldType;
import com.vointika.metafield.infrastructure.persistence.repository.MetaobjectEntryJpaRepository;
import com.vointika.shared.port.StorefrontMetafieldQuery;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * metafield's implementation of the storefront's read.
 *
 * <p>It reads through {@code listForOwnerLocalized}, with the operator as both
 * tenant and owner. <b>That is a different query from the admin editor's</b>, and
 * deliberately so: the editor shows what the operator typed, so overlaying there
 * would make a translated field look canonical and the next save would write the
 * translation back over the original. The ordering promise (namespace, then key)
 * is repeated in both, because it is what stops a storefront payload reshuffling
 * between requests.
 *
 * <p><b>References are resolved here, in one extra query, and only when there is
 * one to resolve.</b> A {@code metaobject_reference} stores an entry id; a theme
 * handed that id has no way to turn it into content, which is what this read
 * served until the entry came with it. The resolve reaches the JPA repository
 * directly rather than through {@code MetaobjectEntryRepository}: it is a
 * storefront-shaped read with no other caller, and the domain repository deals
 * in the entry aggregate rather than a flattened projection.
 */
@Component
public class StorefrontMetafieldQueryImpl implements StorefrontMetafieldQuery {

    private final MetafieldValueRepository valueRepository;
    private final MetaobjectEntryJpaRepository entryJpa;
    private final ObjectMapper objectMapper;

    public StorefrontMetafieldQueryImpl(MetafieldValueRepository valueRepository,
                                        MetaobjectEntryJpaRepository entryJpa,
                                        ObjectMapper objectMapper) {
        this.valueRepository = valueRepository;
        this.entryJpa = entryJpa;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<MetafieldView> findForOperator(UUID tourOperatorId, String locale) {
        List<MetafieldValueWithDefinition> values = valueRepository
                .listForOwnerLocalized(tourOperatorId, MetafieldOwnerType.TOUR_OPERATOR,
                        tourOperatorId, locale);

        Map<UUID, MetaobjectView> resolved = resolve(tourOperatorId, referencedEntryIds(values));

        List<MetafieldView> views = new ArrayList<>(values.size());
        for (MetafieldValueWithDefinition v : values) {
            if (v.type() != MetafieldType.METAOBJECT_REFERENCE) {
                views.add(new MetafieldView(v.namespace(), v.key(), v.type().code(),
                        typed(v.type(), v.value()), null));
                continue;
            }
            UUID id = entryId(v.value());
            // An immutable Map rejects a null key outright, so the unparseable
            // value has to be caught before the lookup, not by it.
            MetaobjectView target = id == null ? null : resolved.get(id);
            // Unpublished, deleted, or another operator's: pruned, never a bare id.
            if (target != null) {
                views.add(new MetafieldView(v.namespace(), v.key(), v.type().code(), v.value(), target));
            }
        }
        return views;
    }

    private static Set<UUID> referencedEntryIds(List<MetafieldValueWithDefinition> values) {
        return values.stream()
                .filter(v -> v.type() == MetafieldType.METAOBJECT_REFERENCE)
                .map(v -> entryId(v.value()))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private Map<UUID, MetaobjectView> resolve(UUID tourOperatorId, Set<UUID> entryIds) {
        if (entryIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, List<PublishedMetaobjectField>> byEntry = entryJpa
                .findPublishedFields(tourOperatorId, entryIds)
                .stream()
                .collect(Collectors.groupingBy(PublishedMetaobjectField::entryId,
                        LinkedHashMap::new, Collectors.toList()));

        Map<UUID, MetaobjectView> views = new LinkedHashMap<>();
        byEntry.forEach((id, rows) -> {
            PublishedMetaobjectField first = rows.getFirst();
            views.put(id, new MetaobjectView(id, first.entryType(), first.entryHandle(), first.entryName(),
                    rows.stream()
                            .map(r -> new MetaobjectFieldView(r.fieldKey(), r.fieldType().code(),
                                    typed(r.fieldType(), r.value())))
                            .toList()));
        });
        return views;
    }

    /**
     * The stored text as the type it actually is.
     *
     * <p><b>This is the only place that can decide it</b>, because
     * {@link MetafieldType} is {@code metafield}'s and no other context may see
     * it — the storefront would have to match on the literals {@code "boolean"}
     * and {@code "json"} instead, and nothing would keep that copy true.
     *
     * <p>Everything else stays text deliberately: numbers because a JSON number
     * is a double on the far side of most parsers and {@code number_integer}
     * already exceeds JavaScript's exact-integer range, dates because JSON has no
     * date type. The reasoning is on {@code StorefrontMetafieldQuery}.
     *
     * <p>{@code Boolean.valueOf} is safe rather than lenient here:
     * {@code MetafieldValueValidator} normalizes to exactly {@code "true"} or
     * {@code "false"} on write, which its javadoc says is so "a later typed
     * exposure never re-parses surprises". This is that exposure.
     */
    private Object typed(MetafieldType type, String raw) {
        return switch (type) {
            case BOOLEAN -> Boolean.valueOf(raw);
            case JSON -> parsedJson(raw);
            default -> raw;
        };
    }

    /**
     * <b>{@code readValue}, never {@code readTree}</b> — the same trap
     * {@code JacksonJsonSyntaxPort} records: {@code readTree} stops at the first
     * complete document and ignores whatever trails it.
     *
     * <p>Falls back to the raw string rather than throwing. The write path
     * validates JSON, so a row that fails here is corruption or predates the
     * validator, and one bad row must not take down every page of a storefront —
     * the rule {@link #entryId} already follows.
     */
    private Object parsedJson(String raw) {
        try {
            return objectMapper.readValue(raw, Object.class);
        } catch (RuntimeException notJson) {
            return raw;
        }
    }

    /**
     * The column is TEXT, so a value that is not a UUID is storable even though
     * the write path validates one. Returning null rather than throwing keeps a
     * single corrupt row from taking down every storefront page.
     */
    private static UUID entryId(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException notAnId) {
            return null;
        }
    }
}

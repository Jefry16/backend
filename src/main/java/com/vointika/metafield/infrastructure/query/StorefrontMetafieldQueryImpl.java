package com.vointika.metafield.infrastructure.query;

import com.vointika.metafield.domain.projection.MetafieldValueWithDefinition;
import com.vointika.metafield.domain.projection.PublishedMetaobjectField;
import com.vointika.metafield.domain.repository.MetafieldValueRepository;
import com.vointika.metafield.domain.valueobject.MetafieldOwnerType;
import com.vointika.metafield.domain.valueobject.MetafieldType;
import com.vointika.metafield.infrastructure.persistence.repository.MetaobjectEntryJpaRepository;
import com.vointika.shared.port.StorefrontMetafieldQuery;
import org.springframework.stereotype.Component;

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
 * <p>It reuses the same {@code listForOwner} the admin editor uses, with the
 * operator as both tenant and owner — the ordering the storefront needs
 * (namespace, then key) is already that query's promise, so there is no second
 * query and no second place for the order to be decided.
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

    public StorefrontMetafieldQueryImpl(MetafieldValueRepository valueRepository,
                                        MetaobjectEntryJpaRepository entryJpa) {
        this.valueRepository = valueRepository;
        this.entryJpa = entryJpa;
    }

    @Override
    public List<MetafieldView> findForOperator(UUID tourOperatorId) {
        List<MetafieldValueWithDefinition> values = valueRepository
                .listForOwner(tourOperatorId, MetafieldOwnerType.TOUR_OPERATOR, tourOperatorId);

        Map<UUID, MetaobjectView> resolved = resolve(tourOperatorId, referencedEntryIds(values));

        List<MetafieldView> views = new ArrayList<>(values.size());
        for (MetafieldValueWithDefinition v : values) {
            if (v.type() != MetafieldType.METAOBJECT_REFERENCE) {
                views.add(new MetafieldView(v.namespace(), v.key(), v.type().code(), v.value(), null));
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
                            .map(r -> new MetaobjectFieldView(r.fieldKey(), r.fieldType().code(), r.value()))
                            .toList()));
        });
        return views;
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

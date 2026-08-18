package com.vointika.metafield.application.usecase;

import com.vointika.metafield.application.dto.output.MetaobjectEntryView;
import com.vointika.metafield.domain.entity.MetaobjectEntry;
import com.vointika.metafield.domain.entity.MetaobjectEntryValue;
import com.vointika.metafield.domain.entity.MetaobjectField;
import com.vointika.metafield.domain.repository.MetaobjectDefinitionRepository;
import com.vointika.metafield.domain.repository.MetaobjectEntryRepository;
import com.vointika.shared.port.TourOperatorMembershipCheck;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A single entry with EVERY field of its definition in position order (value
 * null when unset). Any member; cross-tenant → 404.
 */
public class GetMetaobjectEntryUseCase {

    private final MetaobjectDefinitionRepository definitionRepository;
    private final MetaobjectEntryRepository entryRepository;
    private final TourOperatorMembershipCheck membershipCheck;

    public GetMetaobjectEntryUseCase(MetaobjectDefinitionRepository definitionRepository,
                                     MetaobjectEntryRepository entryRepository,
                                     TourOperatorMembershipCheck membershipCheck) {
        this.definitionRepository = definitionRepository;
        this.entryRepository = entryRepository;
        this.membershipCheck = membershipCheck;
    }

    public MetaobjectEntryView execute(UUID tourOperatorId, UUID entryId, UUID callerUserId) {
        membershipCheck.ensureMember(callerUserId, tourOperatorId);
        MetaobjectEntry entry = entryRepository.requireByIdAndTourOperatorId(entryId, tourOperatorId);

        Map<UUID, String> valuesByFieldId = new HashMap<>();
        for (MetaobjectEntryValue value : entryRepository.valuesOf(entry.getId())) {
            valuesByFieldId.put(value.getFieldDefinitionId(), value.getValue());
        }
        List<MetaobjectEntryView.EntryFieldValue> fields = new ArrayList<>();
        for (MetaobjectField field : definitionRepository.fieldsOf(entry.getDefinitionId())) {
            fields.add(new MetaobjectEntryView.EntryFieldValue(
                    field.getKey().value(), field.getType().code(), field.getName().value(),
                    valuesByFieldId.get(field.getId())));
        }
        return new MetaobjectEntryView(entry, fields);
    }
}

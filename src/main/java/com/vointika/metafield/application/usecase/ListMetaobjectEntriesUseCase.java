package com.vointika.metafield.application.usecase;

import com.vointika.metafield.domain.projection.MetaobjectEntryListItem;
import com.vointika.metafield.domain.repository.MetaobjectEntryRepository;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.ListQuery;
import com.vointika.shared.list.ListSchema;
import com.vointika.shared.port.TourOperatorMembershipCheck;

import java.util.UUID;

/**
 * The operator's metaobject entries — cursor-paginated, tenant-scoped. Any
 * member. Filter by definitionId (the per-type view), name/handle (text) or
 * published; sort by id (default, newest first), name, handle or createdAt.
 */
public class ListMetaobjectEntriesUseCase {

    public static final ListSchema SCHEMA = ListSchema.builder()
            .tenantScoped()
            .set("definitionId", UUID.class)
            .text("name")
            .text("handle")
            .bool("published")
            .instant("createdAt")
            .sortable("id")
            .sortable("name")
            .sortable("handle")
            .sortable("createdAt")
            .defaultSort("-id")
            .build();

    private final MetaobjectEntryRepository entryRepository;
    private final TourOperatorMembershipCheck membershipCheck;

    public ListMetaobjectEntriesUseCase(MetaobjectEntryRepository entryRepository,
                                        TourOperatorMembershipCheck membershipCheck) {
        this.entryRepository = entryRepository;
        this.membershipCheck = membershipCheck;
    }

    public CursorPage<MetaobjectEntryListItem> execute(ListQuery query, UUID callerUserId) {
        membershipCheck.ensureMember(callerUserId, query.tenantId());
        return entryRepository.list(query);
    }
}

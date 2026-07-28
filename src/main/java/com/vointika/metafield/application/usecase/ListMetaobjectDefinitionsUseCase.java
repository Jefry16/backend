package com.vointika.metafield.application.usecase;

import com.vointika.metafield.domain.projection.MetaobjectDefinitionListItem;
import com.vointika.metafield.domain.repository.MetaobjectDefinitionRepository;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.ListQuery;
import com.vointika.shared.list.ListSchema;
import com.vointika.shared.port.TourOperatorMembershipCheck;

import java.util.UUID;

/**
 * The operator's metaobject definitions — cursor-paginated, tenant-scoped.
 * Any member. Filter by type/name (text); sort by id (default, newest
 * first), type, name or createdAt.
 */
public class ListMetaobjectDefinitionsUseCase {

    public static final ListSchema SCHEMA = ListSchema.builder()
            .tenantScoped()
            .text("type")
            .text("name")
            .instant("createdAt")
            .sortable("id")
            .sortable("type")
            .sortable("name")
            .sortable("createdAt")
            .defaultSort("-id")
            .build();

    private final MetaobjectDefinitionRepository definitionRepository;
    private final TourOperatorMembershipCheck membershipCheck;

    public ListMetaobjectDefinitionsUseCase(MetaobjectDefinitionRepository definitionRepository,
                                            TourOperatorMembershipCheck membershipCheck) {
        this.definitionRepository = definitionRepository;
        this.membershipCheck = membershipCheck;
    }

    public CursorPage<MetaobjectDefinitionListItem> execute(ListQuery query, UUID callerUserId) {
        membershipCheck.ensureMember(callerUserId, query.tenantId());
        return definitionRepository.list(query);
    }
}

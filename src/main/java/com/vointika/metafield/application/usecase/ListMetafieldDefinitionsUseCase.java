package com.vointika.metafield.application.usecase;

import com.vointika.metafield.domain.projection.MetafieldDefinitionListItem;
import com.vointika.metafield.domain.repository.MetafieldDefinitionRepository;
import com.vointika.metafield.domain.valueobject.MetafieldOwnerType;
import com.vointika.metafield.domain.valueobject.MetafieldType;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.ListQuery;
import com.vointika.shared.list.ListSchema;
import com.vointika.shared.port.TourOperatorMembershipCheck;

import java.util.UUID;

/**
 * Lists an operator's definitions — cursor-paginated, tenant-scoped. Any
 * member. Filter by namespace/key/name (text) or ownerType/type (set); sort
 * by namespace/name/createdAt/id, default newest first.
 */
public class ListMetafieldDefinitionsUseCase {

    public static final ListSchema SCHEMA = ListSchema.builder()
            .tenantScoped()
            .text("namespace")
            .text("key")
            .text("name")
            .set("ownerType", MetafieldOwnerType.class)
            .set("type", MetafieldType.class)
            .instant("createdAt")
            .sortable("id")
            .sortable("namespace")
            .sortable("name")
            .sortable("createdAt")
            .defaultSort("-id")
            .build();

    private final MetafieldDefinitionRepository definitionRepository;
    private final TourOperatorMembershipCheck membershipCheck;

    public ListMetafieldDefinitionsUseCase(MetafieldDefinitionRepository definitionRepository,
                                           TourOperatorMembershipCheck membershipCheck) {
        this.definitionRepository = definitionRepository;
        this.membershipCheck = membershipCheck;
    }

    public CursorPage<MetafieldDefinitionListItem> execute(ListQuery query, UUID callerUserId) {
        membershipCheck.ensureMember(callerUserId, query.tenantId());
        return definitionRepository.list(query);
    }
}

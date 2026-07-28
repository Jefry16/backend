package com.vointika.contact.application.usecase;

import com.vointika.contact.domain.entity.ContactMessage;
import com.vointika.contact.domain.repository.ContactMessageRepository;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.ListQuery;
import com.vointika.shared.list.ListSchema;
import com.vointika.shared.port.TourOperatorMembershipCheck;

import java.util.UUID;

/**
 * The operator's inbox — cursor-paginated, tenant-scoped, newest first
 * (UUIDv7 ids are time-ordered). Any member. Filter by name/email/summary
 * (text); sort by id (default) or createdAt. Unread state rides the rows
 * (no unread FILTER — the list framework has no bool variant yet, the
 * metaobject-entries precedent).
 */
public class ListContactMessagesUseCase {

    public static final ListSchema SCHEMA = ListSchema.builder()
            .tenantScoped()
            .text("name")
            .text("email")
            .text("summary")
            .instant("createdAt")
            .sortable("id")
            .sortable("createdAt")
            .defaultSort("-id")
            .build();

    private final ContactMessageRepository messageRepository;
    private final TourOperatorMembershipCheck membershipCheck;

    public ListContactMessagesUseCase(ContactMessageRepository messageRepository,
                                      TourOperatorMembershipCheck membershipCheck) {
        this.messageRepository = messageRepository;
        this.membershipCheck = membershipCheck;
    }

    public CursorPage<ContactMessage> execute(ListQuery query, UUID callerUserId) {
        membershipCheck.ensureMember(callerUserId, query.tenantId());
        return messageRepository.list(query);
    }
}

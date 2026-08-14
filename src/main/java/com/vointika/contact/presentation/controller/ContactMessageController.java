package com.vointika.contact.presentation.controller;

import com.vointika.contact.application.usecase.DeleteContactMessageUseCase;
import com.vointika.contact.application.usecase.GetContactMessageUseCase;
import com.vointika.contact.application.usecase.ListContactMessagesUseCase;
import com.vointika.contact.domain.entity.ContactMessage;
import com.vointika.contact.presentation.response.ContactMessageListItemResponse;
import com.vointika.contact.presentation.response.ContactMessageResponse;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.ListQuery;
import com.vointika.shared.web.list.CursorPageResponse;
import com.vointika.shared.web.list.ListQueryParser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * The operator's contact inbox — shopper-submitted messages, read-only.
 * Reads any member (STAFF answers inquiries); delete ADMIN+. The write path is
 * the storefront arc's intake endpoint; rows are dev-seeded until it lands.
 */
@RestController
@RequestMapping("/api/tour-operators/{tourOperatorId}/contact-messages")
public class ContactMessageController {

    private final ListContactMessagesUseCase listUseCase;
    private final GetContactMessageUseCase getUseCase;
    private final DeleteContactMessageUseCase deleteUseCase;
    private final ListQueryParser listQueryParser;

    public ContactMessageController(ListContactMessagesUseCase listUseCase,
                                    GetContactMessageUseCase getUseCase,
                                    DeleteContactMessageUseCase deleteUseCase,
                                    ListQueryParser listQueryParser) {
        this.listUseCase = listUseCase;
        this.getUseCase = getUseCase;
        this.deleteUseCase = deleteUseCase;
        this.listQueryParser = listQueryParser;
    }

    /** The inbox, newest first (filter/sort/cursor per the list framework). Any member. */
    @GetMapping
    public ResponseEntity<CursorPageResponse<ContactMessageListItemResponse>> list(
            @PathVariable UUID tourOperatorId,
            @AuthenticationPrincipal UUID callerUserId,
            HttpServletRequest request) {
        ListQuery query = listQueryParser.parse(
                request, ListContactMessagesUseCase.SCHEMA, tourOperatorId);
        CursorPage<ContactMessage> page = listUseCase.execute(query, callerUserId);
        return ResponseEntity.ok(CursorPageResponse.of(page, ContactMessageListItemResponse::from));
    }

    /** One message with the full body. Any member. */
    @GetMapping("/{messageId}")
    public ResponseEntity<ContactMessageResponse> get(
            @PathVariable UUID tourOperatorId,
            @PathVariable UUID messageId,
            @AuthenticationPrincipal UUID callerUserId) {
        return ResponseEntity.ok(ContactMessageResponse.from(
                getUseCase.execute(tourOperatorId, messageId, callerUserId)));
    }

    /** Hard-deletes a message (spam, handled inquiries). ADMIN+. */
    @DeleteMapping("/{messageId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID tourOperatorId,
            @PathVariable UUID messageId,
            @AuthenticationPrincipal UUID callerUserId) {
        deleteUseCase.execute(tourOperatorId, messageId, callerUserId);
        return ResponseEntity.noContent().build();
    }
}

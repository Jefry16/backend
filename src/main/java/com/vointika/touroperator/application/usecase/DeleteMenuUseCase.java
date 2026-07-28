package com.vointika.touroperator.application.usecase;

import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.valueobject.AuditActor;
import com.vointika.touroperator.domain.entity.Menu;
import com.vointika.touroperator.domain.repository.MenuRepository;

import java.util.Map;
import java.util.UUID;

/**
 * Deletes a menu and its whole item tree (items + translations cascade at DB
 * level). ADMIN+ only. The default menus are ordinary menus — deletable too;
 * the render side treats a missing menu as empty.
 */
public class DeleteMenuUseCase {

    private final MenuRepository menuRepository;
    private final TourOperatorMembershipCheck membershipCheck;
    private final TransactionRunner transactionRunner;
    private final AuditTrailPort auditTrailPort;

    public DeleteMenuUseCase(MenuRepository menuRepository,
                             TourOperatorMembershipCheck membershipCheck,
                             TransactionRunner transactionRunner,
                             AuditTrailPort auditTrailPort) {
        this.menuRepository = menuRepository;
        this.membershipCheck = membershipCheck;
        this.transactionRunner = transactionRunner;
        this.auditTrailPort = auditTrailPort;
    }

    public void execute(UUID tourOperatorId, UUID menuId, UUID callerUserId) {
        membershipCheck.ensureAdmin(callerUserId, tourOperatorId);
        Menu menu = menuRepository.findByIdAndTourOperatorId(menuId, tourOperatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu not found"));

        transactionRunner.run(() -> {
            menuRepository.delete(menu.getId());
            auditTrailPort.append(new NewAuditEntry(
                    tourOperatorId, AuditActor.user(callerUserId),
                    "MENU", menu.getId(), "menu.deleted",
                    Map.of("handle", menu.getHandle().value(), "title", menu.getTitle())));
        });
    }
}

package com.vointika.touroperator.application.usecase;

import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.valueobject.AuditActor;
import com.vointika.shared.valueobject.AuditChanges;
import com.vointika.shared.valueobject.FieldChange;
import com.vointika.touroperator.application.dto.input.RenameMenuInput;
import com.vointika.touroperator.domain.entity.Menu;
import com.vointika.touroperator.domain.repository.MenuRepository;

import java.util.List;
import java.util.Map;

/**
 * Renames a menu (title only — the handle is immutable). ADMIN+ only.
 * A no-op rename saves but records nothing.
 */
public class RenameMenuUseCase {

    private final MenuRepository menuRepository;
    private final TourOperatorMembershipCheck membershipCheck;
    private final TransactionRunner transactionRunner;
    private final AuditTrailPort auditTrailPort;

    public RenameMenuUseCase(MenuRepository menuRepository,
                             TourOperatorMembershipCheck membershipCheck,
                             TransactionRunner transactionRunner,
                             AuditTrailPort auditTrailPort) {
        this.menuRepository = menuRepository;
        this.membershipCheck = membershipCheck;
        this.transactionRunner = transactionRunner;
        this.auditTrailPort = auditTrailPort;
    }

    public void execute(RenameMenuInput input) {
        membershipCheck.ensureAdmin(input.callerUserId(), input.tourOperatorId());
        Menu menu = menuRepository.findByIdAndTourOperatorId(input.menuId(), input.tourOperatorId())
                .orElseThrow(() -> new ResourceNotFoundException("Menu not found"));

        Map<String, Object> before = menu.auditSnapshot();
        menu.rename(input.title());
        List<FieldChange> changes = AuditChanges.diff(before, menu.auditSnapshot());
        transactionRunner.run(() -> {
            menuRepository.save(menu);
            if (!changes.isEmpty()) {
                auditTrailPort.append(new NewAuditEntry(
                        input.tourOperatorId(), AuditActor.user(input.callerUserId()),
                        "MENU", menu.getId(), "menu.renamed",
                        Map.of("handle", menu.getHandle().value()),
                        changes));
            }
        });
    }
}

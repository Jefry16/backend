package com.vointika.touroperator.application.usecase;

import com.vointika.shared.exception.ResourceAlreadyExistsException;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.service.IdGenerator;
import com.vointika.shared.valueobject.AuditActor;
import com.vointika.shared.valueobject.Slug;
import com.vointika.touroperator.application.dto.input.CreateMenuInput;
import com.vointika.touroperator.domain.entity.Menu;
import com.vointika.touroperator.domain.repository.MenuRepository;
import com.vointika.shared.exception.UniqueConstraintViolationException;

import java.util.Map;
import java.util.UUID;

/**
 * Creates an empty menu (handle + title). ADMIN+ only. The handle is the
 * theme-facing identifier — unique per operator, immutable after creation;
 * a duplicate is 409, double-guarded by the DB unique constraint.
 */
public class CreateMenuUseCase {

    private final MenuRepository menuRepository;
    private final TourOperatorMembershipCheck membershipCheck;
    private final IdGenerator idGenerator;
    private final TransactionRunner transactionRunner;
    private final AuditTrailPort auditTrailPort;

    public CreateMenuUseCase(MenuRepository menuRepository,
                             TourOperatorMembershipCheck membershipCheck,
                             IdGenerator idGenerator,
                             TransactionRunner transactionRunner,
                             AuditTrailPort auditTrailPort) {
        this.menuRepository = menuRepository;
        this.membershipCheck = membershipCheck;
        this.idGenerator = idGenerator;
        this.transactionRunner = transactionRunner;
        this.auditTrailPort = auditTrailPort;
    }

    public UUID execute(CreateMenuInput input) {
        membershipCheck.ensureAdmin(input.callerUserId(), input.tourOperatorId());

        Slug handle = new Slug(input.handle());
        Menu menu = new Menu(idGenerator.newId(), input.tourOperatorId(),
                handle, input.title(), input.callerUserId());

        if (menuRepository.existsByTourOperatorIdAndHandle(input.tourOperatorId(), handle.value())) {
            throw new ResourceAlreadyExistsException("A menu with this handle already exists");
        }
        try {
            transactionRunner.run(() -> {
                menuRepository.save(menu);
                auditTrailPort.append(new NewAuditEntry(
                        input.tourOperatorId(), AuditActor.user(input.callerUserId()),
                        "MENU", menu.getId(), "menu.created",
                        Map.of("handle", handle.value(), "title", menu.getTitle())));
            });
        } catch (UniqueConstraintViolationException e) {
            throw new ResourceAlreadyExistsException("A menu with this handle already exists");
        }
        return menu.getId();
    }
}

package com.vointika.experience.application.usecase;

import com.vointika.experience.application.dto.input.CategoryInput;
import com.vointika.experience.domain.entity.Category;
import com.vointika.experience.domain.repository.CategoryRepository;
import com.vointika.experience.domain.valueobject.CategoryName;
import com.vointika.shared.exception.ResourceAlreadyExistsException;
import com.vointika.shared.exception.UniqueConstraintViolationException;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.service.IdGenerator;
import com.vointika.shared.valueobject.AuditActor;

import java.util.UUID;

/**
 * Creates a category. ADMIN+ only; membership enforced by the route interceptor.
 * Names are unique per operator, compared case-insensitively — a duplicate is
 * rejected 409, both up-front and on the unique-index race (never a 500).
 */
public class CreateCategoryUseCase {

    private final CategoryRepository categoryRepository;
    private final TourOperatorMembershipCheck membershipCheck;
    private final IdGenerator idGenerator;
    private final TransactionRunner transactionRunner;
    private final AuditTrailPort auditTrailPort;

    public CreateCategoryUseCase(CategoryRepository categoryRepository,
                                 TourOperatorMembershipCheck membershipCheck,
                                 IdGenerator idGenerator,
                                 TransactionRunner transactionRunner,
                                 AuditTrailPort auditTrailPort) {
        this.categoryRepository = categoryRepository;
        this.membershipCheck = membershipCheck;
        this.idGenerator = idGenerator;
        this.transactionRunner = transactionRunner;
        this.auditTrailPort = auditTrailPort;
    }

    public UUID execute(UUID tourOperatorId, UUID callerUserId, CategoryInput input) {
        membershipCheck.ensureAdmin(callerUserId, tourOperatorId);

        CategoryName name = new CategoryName(input.name());

        if (categoryRepository.existsByTourOperatorIdAndName(tourOperatorId, name.value())) {
            throw new ResourceAlreadyExistsException(CategoryRepository.NAME_TAKEN);
        }

        Category category = new Category(idGenerator.newId(), tourOperatorId, name);
        try {
            return transactionRunner.call(() -> {
                Category saved = categoryRepository.save(category);
                auditTrailPort.append(new NewAuditEntry(
                        tourOperatorId, AuditActor.user(callerUserId),
                        "CATEGORY", saved.getId(), "category.created", null));
                return saved;
            }).getId();
        } catch (UniqueConstraintViolationException e) {
            throw new ResourceAlreadyExistsException(CategoryRepository.NAME_TAKEN);
        }
    }
}

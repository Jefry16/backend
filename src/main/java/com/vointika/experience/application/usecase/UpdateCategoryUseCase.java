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
import com.vointika.shared.valueobject.AuditActor;
import com.vointika.shared.valueobject.AuditChanges;
import com.vointika.shared.valueobject.FieldChange;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Renames a category. ADMIN+ only. Guards: caller not ADMIN+ → 403; id not under
 * this operator → 404; name clashes (case-insensitively) with another of the
 * operator's categories → 409.
 *
 * <p>Partial, like every other PATCH here: an absent name keeps the current one.
 * The name is the only editable field, so an absent one makes the whole call a
 * no-op — nothing written, nothing audited.
 *
 * <p>The experiences filed under the category are untouched. They reference it by
 * id, so a rename is visible everywhere at once and there is no snapshot to
 * propagate — unlike an audience rename, which has frozen slot pricing rows to
 * keep in step.
 */
public class UpdateCategoryUseCase {

    private final CategoryRepository categoryRepository;
    private final TourOperatorMembershipCheck membershipCheck;
    private final TransactionRunner transactionRunner;
    private final AuditTrailPort auditTrailPort;

    public UpdateCategoryUseCase(CategoryRepository categoryRepository,
                                 TourOperatorMembershipCheck membershipCheck,
                                 TransactionRunner transactionRunner,
                                 AuditTrailPort auditTrailPort) {
        this.categoryRepository = categoryRepository;
        this.membershipCheck = membershipCheck;
        this.transactionRunner = transactionRunner;
        this.auditTrailPort = auditTrailPort;
    }

    public void execute(UUID tourOperatorId, UUID categoryId, UUID callerUserId, CategoryInput input) {
        membershipCheck.ensureAdmin(callerUserId, tourOperatorId);

        Category category = categoryRepository.requireByIdAndTourOperatorId(categoryId, tourOperatorId);

        if (input.name() == null) {
            return;
        }

        Map<String, Object> before = category.auditSnapshot();
        CategoryName newName = new CategoryName(input.name());

        // Any change (including case alone) is persisted; the clash check is
        // case-insensitive and excludes this category, so "boat tours" →
        // "Boat Tours" is a rename rather than a self-conflict.
        if (newName.value().equals(category.getName().value())) {
            return;
        }
        if (categoryRepository.existsByTourOperatorIdAndNameExcluding(
                tourOperatorId, newName.value(), categoryId)) {
            throw new ResourceAlreadyExistsException(CategoryRepository.NAME_TAKEN);
        }
        category.rename(newName);

        List<FieldChange> changes = AuditChanges.diff(before, category.auditSnapshot());
        try {
            transactionRunner.run(() -> {
                categoryRepository.save(category);
                auditTrailPort.append(new NewAuditEntry(
                        tourOperatorId, AuditActor.user(callerUserId),
                        "CATEGORY", categoryId, "category.updated", null, changes));
            });
        } catch (UniqueConstraintViolationException e) {
            throw new ResourceAlreadyExistsException(CategoryRepository.NAME_TAKEN);
        }
    }
}

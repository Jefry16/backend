package com.vointika.experience.application.usecase;

import com.vointika.experience.domain.entity.Category;
import com.vointika.experience.domain.repository.CategoryRepository;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.valueobject.AuditActor;

import java.util.Map;
import java.util.UUID;

/**
 * Deletes a category. ADMIN+ only. An id not under this operator → 404.
 *
 * <p><b>The experiences filed under it survive and become uncategorized.</b>
 * {@code experiences.category_id} is {@code ON DELETE SET NULL}, so the database
 * performs that sweep inside this transaction — there is no application-side
 * reassignment and no refusal while the category is in use. An uncategorized
 * experience is the state it was in before anyone categorized it, so nothing is
 * lost but the classification.
 *
 * <p>The name goes into the audit entry rather than being left to the id: the row
 * is gone afterwards, so the trail is the only place the operator can still read
 * what was deleted.
 */
public class DeleteCategoryUseCase {

    private final CategoryRepository categoryRepository;
    private final TourOperatorMembershipCheck membershipCheck;
    private final TransactionRunner transactionRunner;
    private final AuditTrailPort auditTrailPort;

    public DeleteCategoryUseCase(CategoryRepository categoryRepository,
                                 TourOperatorMembershipCheck membershipCheck,
                                 TransactionRunner transactionRunner,
                                 AuditTrailPort auditTrailPort) {
        this.categoryRepository = categoryRepository;
        this.membershipCheck = membershipCheck;
        this.transactionRunner = transactionRunner;
        this.auditTrailPort = auditTrailPort;
    }

    public void execute(UUID tourOperatorId, UUID categoryId, UUID callerUserId) {
        membershipCheck.ensureAdmin(callerUserId, tourOperatorId);
        Category category = categoryRepository.requireByIdAndTourOperatorId(categoryId, tourOperatorId);

        transactionRunner.run(() -> {
            categoryRepository.delete(category);
            auditTrailPort.append(new NewAuditEntry(
                    tourOperatorId, AuditActor.user(callerUserId),
                    "CATEGORY", categoryId, "category.deleted",
                    Map.of("name", category.getName().value())));
        });
    }
}

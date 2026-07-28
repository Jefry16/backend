package com.vointika.touroperator.application.usecase;

import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.ExperienceOwnershipQuery;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.PageOwnershipQuery;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.service.IdGenerator;
import com.vointika.shared.valueobject.AuditActor;
import com.vointika.shared.valueobject.LocaleCode;
import com.vointika.touroperator.application.dto.input.ReplaceMenuItemsInput;
import com.vointika.touroperator.application.dto.input.ReplaceMenuItemsInput.MenuItemInput;
import com.vointika.touroperator.domain.entity.Menu;
import com.vointika.touroperator.domain.entity.MenuItem;
import com.vointika.touroperator.domain.entity.MenuItemTranslation;
import com.vointika.touroperator.domain.enums.MenuItemLinkType;
import com.vointika.touroperator.domain.repository.MenuItemRepository;
import com.vointika.touroperator.domain.repository.MenuRepository;
import com.vointika.touroperator.domain.repository.TourOperatorRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Replaces a menu's entire item tree — the navigation editor's save. ADMIN+
 * only. There is no per-item CRUD (Shopify's write model): the submitted tree
 * is flattened, assigning {@code position} from array order and parent/depth
 * from nesting, and every node is validated <b>before</b> any write — depth
 * cap and link-type/payload rules via {@link MenuItem#create}, EXPERIENCE/PAGE
 * targets against the ownership ports (not this operator's → 422), translation
 * locales against the operator's supported set. The clear + insert then run in
 * one transaction, so a rejected tree leaves the existing menu untouched.
 *
 * <p>Items get fresh ids every save, which is why translations ride inline
 * (a standalone translation resource would be orphaned on the next save) and
 * why the audit trail records ONE {@code menu.items_replaced} event, not
 * per-item diffs.
 */
public class ReplaceMenuItemsUseCase {

    private final MenuRepository menuRepository;
    private final MenuItemRepository menuItemRepository;
    private final TourOperatorRepository tourOperatorRepository;
    private final ExperienceOwnershipQuery experienceOwnershipQuery;
    private final PageOwnershipQuery pageOwnershipQuery;
    private final TourOperatorMembershipCheck membershipCheck;
    private final IdGenerator idGenerator;
    private final TransactionRunner transactionRunner;
    private final AuditTrailPort auditTrailPort;

    public ReplaceMenuItemsUseCase(MenuRepository menuRepository,
                                   MenuItemRepository menuItemRepository,
                                   TourOperatorRepository tourOperatorRepository,
                                   ExperienceOwnershipQuery experienceOwnershipQuery,
                                   PageOwnershipQuery pageOwnershipQuery,
                                   TourOperatorMembershipCheck membershipCheck,
                                   IdGenerator idGenerator,
                                   TransactionRunner transactionRunner,
                                   AuditTrailPort auditTrailPort) {
        this.menuRepository = menuRepository;
        this.menuItemRepository = menuItemRepository;
        this.tourOperatorRepository = tourOperatorRepository;
        this.experienceOwnershipQuery = experienceOwnershipQuery;
        this.pageOwnershipQuery = pageOwnershipQuery;
        this.membershipCheck = membershipCheck;
        this.idGenerator = idGenerator;
        this.transactionRunner = transactionRunner;
        this.auditTrailPort = auditTrailPort;
    }

    public void execute(ReplaceMenuItemsInput input) {
        membershipCheck.ensureAdmin(input.callerUserId(), input.tourOperatorId());
        Menu menu = menuRepository.findByIdAndTourOperatorId(input.menuId(), input.tourOperatorId())
                .orElseThrow(() -> new ResourceNotFoundException("Menu not found"));

        Set<String> supportedLocales = tourOperatorRepository.findById(input.tourOperatorId())
                .map(operator -> operator.getSupportedLocales().stream()
                        .map(LocaleCode::value)
                        .collect(Collectors.toSet()))
                .orElseThrow(() -> new ResourceNotFoundException("Tour operator not found"));

        List<MenuItem> items = new ArrayList<>();
        List<MenuItemTranslation> translations = new ArrayList<>();
        flatten(input.items(), menu.getId(), null, 0, Instant.now(),
                supportedLocales, items, translations);
        validateResourceTargets(items, input.tourOperatorId());

        transactionRunner.run(() -> {
            menuItemRepository.deleteByMenuId(menu.getId()); // cascades to translations
            menuItemRepository.saveAll(items);
            menuItemRepository.saveAllTranslations(translations);
            auditTrailPort.append(new NewAuditEntry(
                    input.tourOperatorId(), AuditActor.user(input.callerUserId()),
                    "MENU", menu.getId(), "menu.items_replaced",
                    Map.of("handle", menu.getHandle().value(),
                            "itemCount", items.size())));
        });
    }

    /** Depth-first flatten: position = sibling index, parentDepth = parent's depth. */
    private void flatten(List<MenuItemInput> nodes, UUID menuId, UUID parentId,
                         int parentDepth, Instant now, Set<String> supportedLocales,
                         List<MenuItem> outItems, List<MenuItemTranslation> outTranslations) {
        if (nodes == null) {
            return;
        }
        for (int position = 0; position < nodes.size(); position++) {
            MenuItemInput node = nodes.get(position);
            UUID id = idGenerator.newId();
            outItems.add(MenuItem.create(
                    id, menuId, parentId, parentDepth,
                    node.title(), parseLinkType(node.linkType()),
                    node.resourceId(), node.url(), position, now));
            addTranslations(id, node.titleTranslations(), supportedLocales, outTranslations);
            flatten(node.children(), menuId, id, parentDepth + 1, now,
                    supportedLocales, outItems, outTranslations);
        }
    }

    private static void addTranslations(UUID itemId, Map<String, String> titleTranslations,
                                        Set<String> supportedLocales,
                                        List<MenuItemTranslation> out) {
        if (titleTranslations == null) {
            return;
        }
        for (Map.Entry<String, String> entry : titleTranslations.entrySet()) {
            LocaleCode locale = new LocaleCode(entry.getKey());
            if (!supportedLocales.contains(locale.value())) {
                throw new InvalidFieldException(
                        "Locale '" + locale.value() + "' is not one of the operator's supported locales");
            }
            out.add(new MenuItemTranslation(itemId, locale, entry.getValue()));
        }
    }

    /** EXPERIENCE/PAGE targets must exist AND belong to this operator (else 422). */
    private void validateResourceTargets(List<MenuItem> items, UUID tourOperatorId) {
        Set<UUID> checkedExperiences = new HashSet<>();
        Set<UUID> checkedPages = new HashSet<>();
        for (MenuItem item : items) {
            switch (item.getLinkType()) {
                case EXPERIENCE -> {
                    if (checkedExperiences.add(item.getResourceId())
                            && !experienceOwnershipQuery.existsForTourOperator(
                                    item.getResourceId(), tourOperatorId)) {
                        throw new InvalidFieldException(
                                "EXPERIENCE link must reference one of the operator's experiences");
                    }
                }
                case PAGE -> {
                    if (checkedPages.add(item.getResourceId())
                            && !pageOwnershipQuery.existsForTourOperator(
                                    item.getResourceId(), tourOperatorId)) {
                        throw new InvalidFieldException(
                                "PAGE link must reference one of the operator's pages");
                    }
                }
                default -> { }
            }
        }
    }

    private static MenuItemLinkType parseLinkType(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidFieldException("Menu item link type is required");
        }
        try {
            return MenuItemLinkType.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new InvalidFieldException("Unknown menu item link type: " + value);
        }
    }
}

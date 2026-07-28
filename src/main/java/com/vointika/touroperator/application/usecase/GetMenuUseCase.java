package com.vointika.touroperator.application.usecase;

import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.touroperator.application.dto.output.MenuDetail;
import com.vointika.touroperator.application.dto.output.MenuItemNode;
import com.vointika.touroperator.domain.entity.Menu;
import com.vointika.touroperator.domain.entity.MenuItem;
import com.vointika.touroperator.domain.entity.MenuItemTranslation;
import com.vointika.touroperator.domain.repository.MenuItemRepository;
import com.vointika.touroperator.domain.repository.MenuRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * One menu with its full item tree (children ordered by position,
 * translations inline). Any member.
 */
public class GetMenuUseCase {

    private final MenuRepository menuRepository;
    private final MenuItemRepository menuItemRepository;
    private final TourOperatorMembershipCheck membershipCheck;

    public GetMenuUseCase(MenuRepository menuRepository,
                          MenuItemRepository menuItemRepository,
                          TourOperatorMembershipCheck membershipCheck) {
        this.menuRepository = menuRepository;
        this.menuItemRepository = menuItemRepository;
        this.membershipCheck = membershipCheck;
    }

    public MenuDetail execute(UUID tourOperatorId, UUID menuId, UUID callerUserId) {
        membershipCheck.ensureMember(callerUserId, tourOperatorId);
        Menu menu = menuRepository.findByIdAndTourOperatorId(menuId, tourOperatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu not found"));

        List<MenuItem> items = menuItemRepository.findByMenuId(menu.getId());

        Map<UUID, Map<String, String>> translationsByItem = new HashMap<>();
        for (MenuItemTranslation translation : menuItemRepository.findTranslationsByMenuId(menu.getId())) {
            translationsByItem
                    .computeIfAbsent(translation.menuItemId(), k -> new LinkedHashMap<>())
                    .put(translation.locale().value(), translation.title());
        }

        return new MenuDetail(menu, assemble(items, null, translationsByItem));
    }

    /** Builds the sibling list under {@code parentId}; items arrive position-ordered. */
    private static List<MenuItemNode> assemble(List<MenuItem> items, UUID parentId,
                                               Map<UUID, Map<String, String>> translationsByItem) {
        List<MenuItemNode> nodes = new ArrayList<>();
        for (MenuItem item : items) {
            if (!Objects.equals(item.getParentId(), parentId)) {
                continue;
            }
            nodes.add(new MenuItemNode(
                    item.getId(), item.getTitle(), item.getLinkType(),
                    item.getResourceId(), item.getUrl(),
                    translationsByItem.getOrDefault(item.getId(), Map.of()),
                    assemble(items, item.getId(), translationsByItem)));
        }
        return nodes;
    }
}

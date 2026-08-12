package com.vointika.touroperator.infrastructure.query;

import com.vointika.shared.port.StorefrontMenuQuery.MenuView;
import com.vointika.touroperator.infrastructure.persistence.entity.MenuItemJpaEntity;
import com.vointika.touroperator.infrastructure.persistence.entity.MenuItemTranslationJpaEntity;
import com.vointika.touroperator.infrastructure.persistence.entity.MenuJpaEntity;
import com.vointika.touroperator.infrastructure.persistence.repository.MenuItemJpaRepository;
import com.vointika.touroperator.infrastructure.persistence.repository.MenuItemTranslationJpaRepository;
import com.vointika.touroperator.infrastructure.persistence.repository.MenuJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * The navigation read. What nothing above it can see is the query count — an
 * operator's whole navigation in three reads, not one per menu and not one per
 * item.
 */
class StorefrontMenuQueryImplTest {

    private static final UUID OPERATOR = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3ea2");
    private static final UUID MAIN = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3e01");
    private static final UUID FOOTER = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3e02");
    private static final UUID ITEM = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3e03");

    private MenuJpaRepository menuRepository;
    private MenuItemJpaRepository itemRepository;
    private MenuItemTranslationJpaRepository translationRepository;
    private StorefrontMenuQueryImpl query;

    @BeforeEach
    void setUp() {
        menuRepository = mock(MenuJpaRepository.class);
        itemRepository = mock(MenuItemJpaRepository.class);
        translationRepository = mock(MenuItemTranslationJpaRepository.class);
        query = new StorefrontMenuQueryImpl(menuRepository, itemRepository, translationRepository);
    }

    private static MenuJpaEntity menu(UUID id, String handle, String title) {
        MenuJpaEntity menu = mock(MenuJpaEntity.class);
        when(menu.getId()).thenReturn(id);
        when(menu.getHandle()).thenReturn(handle);
        when(menu.getTitle()).thenReturn(title);
        return menu;
    }

    private static MenuItemJpaEntity item(UUID id, UUID menuId, String title) {
        MenuItemJpaEntity item = mock(MenuItemJpaEntity.class);
        when(item.getId()).thenReturn(id);
        when(item.getMenuId()).thenReturn(menuId);
        when(item.getTitle()).thenReturn(title);
        when(item.getLinkType()).thenReturn("HOME");
        return item;
    }

    /** Both menus every operator has, each with its own items, from one item read. */
    @Test
    void itemsAreDealtBackToTheMenuTheyBelongTo() {
        MenuJpaEntity main = menu(MAIN, "main-menu", "Main menu");
        MenuJpaEntity footer = menu(FOOTER, "footer", "Footer");
        MenuItemJpaEntity mainItem = item(ITEM, MAIN, "Home");
        when(menuRepository.findByTourOperatorIdOrderByHandleAsc(OPERATOR))
                .thenReturn(List.of(footer, main));
        when(itemRepository.findByMenuIdInOrderByMenuIdAscPositionAsc(any()))
                .thenReturn(List.of(mainItem));
        when(translationRepository.findByMenuItemIdInAndLocale(any(), anyString()))
                .thenReturn(List.of());

        List<MenuView> menus = query.findMenus(OPERATOR, "es");

        assertThat(menus).extracting(MenuView::handle).containsExactly("footer", "main-menu");
        assertThat(menus.get(0).items()).isEmpty();
        assertThat(menus.get(1).items()).extracting("title").containsExactly("Home");
    }

    /** An item title falls back to the canonical when this locale has no row. */
    @Test
    void itemTitlesOverlayNullableWinsCanonical() {
        MenuJpaEntity main = menu(MAIN, "main-menu", "Main menu");
        MenuItemJpaEntity mainItem = item(ITEM, MAIN, "Home");
        MenuItemTranslationJpaEntity spanish = mock(MenuItemTranslationJpaEntity.class);
        when(spanish.getMenuItemId()).thenReturn(ITEM);
        when(spanish.getTitle()).thenReturn("Inicio");

        when(menuRepository.findByTourOperatorIdOrderByHandleAsc(OPERATOR)).thenReturn(List.of(main));
        when(itemRepository.findByMenuIdInOrderByMenuIdAscPositionAsc(any())).thenReturn(List.of(mainItem));
        when(translationRepository.findByMenuItemIdInAndLocale(any(), anyString()))
                .thenReturn(List.of(spanish));

        assertThat(query.findMenus(OPERATOR, "es").getFirst().items())
                .extracting("title").containsExactly("Inicio");
    }

    /** No menus means no item read and no translation read. */
    @Test
    void anOperatorWithNoMenusReadsNothingElse() {
        when(menuRepository.findByTourOperatorIdOrderByHandleAsc(OPERATOR)).thenReturn(List.of());

        assertThat(query.findMenus(OPERATOR, "es")).isEmpty();
        verifyNoInteractions(itemRepository, translationRepository);
    }

    /** Menus with no items skip the translation read too — there is nothing to translate. */
    @Test
    void emptyMenusSkipTheTranslationRead() {
        MenuJpaEntity main = menu(MAIN, "main-menu", "Main menu");
        when(menuRepository.findByTourOperatorIdOrderByHandleAsc(OPERATOR)).thenReturn(List.of(main));
        when(itemRepository.findByMenuIdInOrderByMenuIdAscPositionAsc(any())).thenReturn(List.of());

        assertThat(query.findMenus(OPERATOR, "es")).singleElement()
                .satisfies(menu -> assertThat(menu.items()).isEmpty());
        verifyNoInteractions(translationRepository);
    }
}

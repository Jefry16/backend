package com.vointika.rendering.application.service;

import com.vointika.rendering.application.dto.output.NavigationItem;
import com.vointika.rendering.application.dto.output.NavigationMenu;
import com.vointika.shared.port.NavigationItemView;
import com.vointika.shared.port.NavigationMenuView;
import com.vointika.shared.port.StorefrontExperienceQuery;
import com.vointika.shared.port.StorefrontNavigationQuery;
import com.vointika.shared.port.StorefrontPageQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NavigationAssemblerTest {

    private static final UUID OP = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3ea2");
    private static final UUID EXPERIENCE = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3e01");
    private static final UUID PAGE = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3e02");

    private StorefrontNavigationQuery navigationQuery;
    private StorefrontExperienceQuery experienceQuery;
    private StorefrontPageQuery pageQuery;
    private NavigationAssembler assembler;

    @BeforeEach
    void setUp() {
        navigationQuery = mock(StorefrontNavigationQuery.class);
        experienceQuery = mock(StorefrontExperienceQuery.class);
        pageQuery = mock(StorefrontPageQuery.class);
        when(experienceQuery.publishedHandles(any(), anyCollection(), any())).thenReturn(Map.of());
        when(pageQuery.publishedHandles(any(), anyCollection(), any())).thenReturn(Map.of());
        assembler = new NavigationAssembler(navigationQuery, experienceQuery, pageQuery);
    }

    private NavigationItemView item(String title, String type, UUID resourceId, String url,
                                    NavigationItemView... children) {
        return new NavigationItemView(title, type, resourceId, url, List.of(children));
    }

    private void givenMenu(NavigationItemView... items) {
        when(navigationQuery.findMenus(OP, "en"))
                .thenReturn(List.of(new NavigationMenuView("main-menu", "Main menu", List.of(items))));
    }

    @Test
    void resolves_an_experience_item_to_its_handle_for_the_locale() {
        givenMenu(item("Dive", "EXPERIENCE", EXPERIENCE, null));
        when(experienceQuery.publishedHandles(OP, java.util.Set.of(EXPERIENCE), "en"))
                .thenReturn(Map.of(EXPERIENCE, "morning-dive"));

        List<NavigationMenu> menus = assembler.assemble(OP, "en");

        assertThat(menus).singleElement().satisfies(menu -> {
            assertThat(menu.handle()).isEqualTo("main-menu");
            assertThat(menu.items()).singleElement()
                    .extracting(NavigationItem::handle).isEqualTo("morning-dive");
        });
    }

    @Test
    void keeps_items_that_need_no_target() {
        givenMenu(
                item("Home", "HOME", null, null),
                item("All experiences", "EXPERIENCE_LIST", null, null),
                item("Instagram", "EXTERNAL_URL", null, "https://instagram.com/acme"));

        List<NavigationItem> items = assembler.assemble(OP, "en").getFirst().items();

        assertThat(items).hasSize(3);
        assertThat(items).allSatisfy(item -> assertThat(item.handle()).isNull());
        assertThat(items.get(2).externalUrl()).isEqualTo("https://instagram.com/acme");
    }

    @Test
    void drops_an_item_whose_target_is_not_published() {
        // Unresolvable means unpublished, deleted, or another tenant's. Rendering
        // it would put a link to a 404 in the header of every page.
        givenMenu(
                item("Home", "HOME", null, null),
                item("Secret draft", "EXPERIENCE", EXPERIENCE, null));

        List<NavigationItem> items = assembler.assemble(OP, "en").getFirst().items();

        assertThat(items).singleElement().extracting(NavigationItem::title).isEqualTo("Home");
    }

    @Test
    void drops_a_page_item_whose_target_is_not_published() {
        givenMenu(item("About", "PAGE", PAGE, null));

        assertThat(assembler.assemble(OP, "en").getFirst().items()).isEmpty();
    }

    @Test
    void resolves_nested_children_too() {
        givenMenu(item("More", "HOME", null, null,
                item("Dive", "EXPERIENCE", EXPERIENCE, null),
                item("About", "PAGE", PAGE, null)));
        when(experienceQuery.publishedHandles(any(), anyCollection(), any()))
                .thenReturn(Map.of(EXPERIENCE, "morning-dive"));
        when(pageQuery.publishedHandles(any(), anyCollection(), any()))
                .thenReturn(Map.of(PAGE, "about-us"));

        List<NavigationItem> children = assembler.assemble(OP, "en").getFirst().items()
                .getFirst().children();

        assertThat(children).extracting(NavigationItem::handle)
                .containsExactly("morning-dive", "about-us");
    }

    @Test
    void a_dead_child_drops_without_taking_its_siblings() {
        givenMenu(item("More", "HOME", null, null,
                item("Dive", "EXPERIENCE", EXPERIENCE, null),
                item("Gone", "PAGE", PAGE, null)));
        when(experienceQuery.publishedHandles(any(), anyCollection(), any()))
                .thenReturn(Map.of(EXPERIENCE, "morning-dive"));

        List<NavigationItem> children = assembler.assemble(OP, "en").getFirst().items()
                .getFirst().children();

        assertThat(children).extracting(NavigationItem::title).containsExactly("Dive");
    }

    @Test
    void resolves_every_target_in_two_queries_however_big_the_menu() {
        // Navigation renders on every page, so an N+1 here is paid by every request.
        givenMenu(
                item("A", "EXPERIENCE", EXPERIENCE, null),
                item("B", "PAGE", PAGE, null),
                item("C", "EXPERIENCE", EXPERIENCE, null, item("D", "PAGE", PAGE, null)));

        assembler.assemble(OP, "en");

        verify(experienceQuery).publishedHandles(any(), anyCollection(), any());
        verify(pageQuery).publishedHandles(any(), anyCollection(), any());
    }

    @Test
    void an_operator_with_no_menus_costs_no_resolution() {
        when(navigationQuery.findMenus(OP, "en")).thenReturn(List.of());

        assertThat(assembler.assemble(OP, "en")).isEmpty();
    }
}

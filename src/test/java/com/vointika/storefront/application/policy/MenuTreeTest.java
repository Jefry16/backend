package com.vointika.storefront.application.policy;

import com.vointika.shared.port.StorefrontMenuQuery.MenuItemView;
import com.vointika.storefront.application.dto.output.MenuData.MenuLinkData;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The rule that decides what a visitor sees in the header, so every case here is
 * a link that appears or does not.
 */
class MenuTreeTest {

    private static final UUID PARENT = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3e01");
    private static final UUID CHILD = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3e02");
    private static final UUID GRANDCHILD = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3e03");
    private static final UUID EXPERIENCE = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3e10");
    private static final UUID PAGE = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3e11");

    private static MenuItemView item(UUID id, UUID parentId, String title, String type,
                                     UUID resourceId, String url, int position) {
        return new MenuItemView(id, parentId, title, type, resourceId, url, position);
    }

    @Test
    void routeLinksAlwaysResolve() {
        List<MenuLinkData> links = MenuTree.build(
                List.of(item(PARENT, null, "Home", "HOME", null, null, 0),
                        item(CHILD, null, "All trips", "EXPERIENCE_LIST", null, null, 1)),
                Map.of(), Map.of());

        assertThat(links).extracting(MenuLinkData::linkType)
                .containsExactly("HOME", "EXPERIENCE_LIST");
    }

    @Test
    void aResourceLinkResolvesToItsHandle() {
        List<MenuLinkData> links = MenuTree.build(
                List.of(item(PARENT, null, "Sunset sail", "EXPERIENCE", EXPERIENCE, null, 0),
                        item(CHILD, null, "About", "PAGE", PAGE, null, 1)),
                Map.of(EXPERIENCE, "sunset-sail"), Map.of(PAGE, "about-us"));

        assertThat(links).extracting(MenuLinkData::targetHandle)
                .containsExactly("sunset-sail", "about-us");
    }

    /**
     * <b>The decision this class exists for.</b> Unpublishing is how an operator
     * takes something off the storefront, so a link to it has to go with it —
     * leaving it would defeat the act and hand a visitor a 404 nobody chose.
     */
    @Test
    void aLinkToAnUnpublishedTargetIsDropped() {
        List<MenuLinkData> links = MenuTree.build(
                List.of(item(PARENT, null, "Sunset sail", "EXPERIENCE", EXPERIENCE, null, 0),
                        item(CHILD, null, "Draft page", "PAGE", PAGE, null, 1),
                        item(GRANDCHILD, null, "Home", "HOME", null, null, 2)),
                Map.of(), Map.of());

        assertThat(links).extracting(MenuLinkData::title).containsExactly("Home");
    }

    /**
     * A child menu hangs off its parent. Promoting orphans into the top level
     * would invent navigation the operator never arranged.
     */
    @Test
    void aDroppedParentTakesItsChildrenWithIt() {
        List<MenuLinkData> links = MenuTree.build(
                List.of(item(PARENT, null, "Gone", "PAGE", PAGE, null, 0),
                        item(CHILD, PARENT, "Still here?", "HOME", null, null, 0),
                        item(GRANDCHILD, CHILD, "Deeper", "HOME", null, null, 0)),
                Map.of(), Map.of());

        assertThat(links).isEmpty();
    }

    @Test
    void theTreeKeepsTheOperatorsNesting() {
        List<MenuLinkData> links = MenuTree.build(
                List.of(item(PARENT, null, "Trips", "EXPERIENCE_LIST", null, null, 0),
                        item(CHILD, PARENT, "Sunset sail", "EXPERIENCE", EXPERIENCE, null, 0),
                        item(GRANDCHILD, CHILD, "About", "PAGE", PAGE, null, 0)),
                Map.of(EXPERIENCE, "sunset-sail"), Map.of(PAGE, "about-us"));

        assertThat(links).singleElement().satisfies(top -> {
            assertThat(top.title()).isEqualTo("Trips");
            assertThat(top.links()).singleElement().satisfies(child -> {
                assertThat(child.title()).isEqualTo("Sunset sail");
                assertThat(child.links()).extracting(MenuLinkData::title).containsExactly("About");
            });
        });
    }

    /** An external link is the operator's own, off-site, and nothing here can verify it. */
    @Test
    void anExternalLinkPassesThrough() {
        List<MenuLinkData> links = MenuTree.build(
                List.of(item(PARENT, null, "Blog", "EXTERNAL_URL", null, "https://example.com/blog", 0)),
                Map.of(), Map.of());

        assertThat(links).singleElement()
                .satisfies(link -> assertThat(link.externalUrl()).isEqualTo("https://example.com/blog"));
    }

    /** The port hands items back position-ordered; the tree must not reshuffle them. */
    @Test
    void siblingsKeepTheirOrder() {
        List<MenuLinkData> links = MenuTree.build(
                List.of(item(PARENT, null, "First", "HOME", null, null, 0),
                        item(CHILD, null, "Second", "EXPERIENCE_LIST", null, null, 1),
                        item(GRANDCHILD, null, "Third", "EXTERNAL_URL", null, "https://x.test", 2)),
                Map.of(), Map.of());

        assertThat(links).extracting(MenuLinkData::title).containsExactly("First", "Second", "Third");
    }

    @Test
    void anEmptyMenuIsAnEmptyList() {
        assertThat(MenuTree.build(List.of(), Map.of(), Map.of())).isEmpty();
    }
}

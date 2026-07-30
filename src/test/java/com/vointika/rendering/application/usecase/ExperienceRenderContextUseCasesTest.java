package com.vointika.rendering.application.usecase;

import com.vointika.rendering.application.dto.output.ExperienceListRenderContext;
import com.vointika.rendering.application.service.TenantResolver;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.StorefrontExperienceQuery;
import com.vointika.shared.port.StorefrontExperienceView;
import com.vointika.shared.port.StorefrontOperatorQuery;
import com.vointika.shared.port.StorefrontOperatorView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExperienceRenderContextUseCasesTest {

    private static final String SLUG = "acme";
    private static final UUID OP = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3ea2");

    private StorefrontOperatorQuery operatorQuery;
    private StorefrontExperienceQuery experienceQuery;
    private GetExperienceListRenderContextUseCase listUseCase;
    private GetExperienceRenderContextUseCase getUseCase;

    @BeforeEach
    void setUp() {
        operatorQuery = mock(StorefrontOperatorQuery.class);
        experienceQuery = mock(StorefrontExperienceQuery.class);
        TenantResolver tenantResolver = new TenantResolver(operatorQuery);
        listUseCase = new GetExperienceListRenderContextUseCase(tenantResolver, experienceQuery);
        getUseCase = new GetExperienceRenderContextUseCase(tenantResolver, experienceQuery);

        when(operatorQuery.findBySlug(SLUG)).thenReturn(Optional.of(new StorefrontOperatorView(
                OP, "Acme Tours", SLUG, null, "en", List.of("en", "es"),
                "EUR", "Europe/Madrid", false, null)));
    }

    private StorefrontExperienceView experience(String slug, String name) {
        return new StorefrontExperienceView(slug, name, "desc", "long", List.of(), List.of(),
                List.of(), List.of(), null, List.of(), 90, false, slug, Map.of());
    }

    @Test
    void list_returns_the_tenant_its_locale_and_its_published_experiences() {
        when(experienceQuery.listPublished(OP, "en", null))
                .thenReturn(new CursorPage<>(List.of(experience("morning-dive", "Morning dive")), "cur-2"));

        ExperienceListRenderContext context = listUseCase.execute(SLUG, null, null);

        assertThat(context.shop().name()).isEqualTo("Acme Tours");
        assertThat(context.locale()).isEqualTo("en");
        assertThat(context.experiences().data()).singleElement()
                .extracting(StorefrontExperienceView::slug).isEqualTo("morning-dive");
        assertThat(context.experiences().nextCursor()).isEqualTo("cur-2");
    }

    @Test
    void list_asks_for_content_in_the_resolved_locale_not_the_requested_one() {
        when(experienceQuery.listPublished(OP, "en", null)).thenReturn(CursorPage.empty());

        // French is not published, so the page renders in the primary locale —
        // and the content must be fetched in that same locale, or the chrome and
        // the experiences would disagree.
        listUseCase.execute(SLUG, "fr", null);

        verify(experienceQuery).listPublished(OP, "en", null);
    }

    @Test
    void list_of_an_operator_with_nothing_published_is_empty_not_missing() {
        when(experienceQuery.listPublished(OP, "en", null)).thenReturn(CursorPage.empty());

        assertThat(listUseCase.execute(SLUG, null, null).experiences().data()).isEmpty();
    }

    @Test
    void list_for_an_unknown_tenant_is_not_found() {
        when(operatorQuery.findBySlug("nobody")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> listUseCase.execute("nobody", null, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void list_passes_the_page_cursor_through_untouched() {
        when(experienceQuery.listPublished(OP, "en", "cur-2")).thenReturn(CursorPage.empty());

        listUseCase.execute(SLUG, null, "cur-2");

        verify(experienceQuery).listPublished(OP, "en", "cur-2");
    }

    @Test
    void get_returns_the_experience_addressed_by_its_handle() {
        when(experienceQuery.findPublishedBySlug(OP, "morning-dive", "en"))
                .thenReturn(Optional.of(experience("morning-dive", "Morning dive")));

        assertThat(getUseCase.execute(SLUG, "morning-dive", null).experience().name())
                .isEqualTo("Morning dive");
    }

    @Test
    void get_resolves_the_handle_in_the_page_locale() {
        when(experienceQuery.findPublishedBySlug(OP, "buceo-matutino", "es"))
                .thenReturn(Optional.of(experience("buceo-matutino", "Buceo matutino")));

        assertThat(getUseCase.execute(SLUG, "buceo-matutino", "es").locale()).isEqualTo("es");
    }

    @Test
    void get_of_an_unknown_or_unpublished_handle_is_the_same_not_found() {
        // Indistinguishable by design: a distinct response would let anyone
        // confirm a draft exists by guessing its slug.
        when(experienceQuery.findPublishedBySlug(OP, "secret-draft", "en"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> getUseCase.execute(SLUG, "secret-draft", null))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}

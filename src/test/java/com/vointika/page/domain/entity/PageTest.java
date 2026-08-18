package com.vointika.page.domain.entity;

import com.vointika.page.domain.valueobject.PageBody;
import com.vointika.shared.valueobject.SeoDescription;
import com.vointika.shared.valueobject.SeoTitle;
import com.vointika.page.domain.valueobject.PageTitle;
import com.vointika.shared.exception.ConflictException;
import com.vointika.shared.valueobject.Handle;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PageTest {

    private Page page() {
        return new Page(UUID.randomUUID(), UUID.randomUUID(),
                new PageTitle("About us"), new Handle("about-us"),
                new PageBody("<p>Hello</p>"),
                new SeoTitle("About"), new SeoDescription("Who we are"),
                UUID.randomUUID());
    }

    @Test
    void newPageStartsDraftAndTransitionsAreGuarded() {
        Page p = page();
        assertThat(p.isPublished()).isFalse();
        assertThatThrownBy(p::unpublish).isInstanceOf(ConflictException.class);
        p.publish();
        assertThat(p.isPublished()).isTrue();
        assertThatThrownBy(p::publish).isInstanceOf(ConflictException.class);
        p.unpublish();
        assertThat(p.isPublished()).isFalse();
    }

    @Test
    void auditSnapshotExposesExactlyTheAuditedFields() {
        Map<String, Object> snapshot = page().auditSnapshot();
        assertThat(snapshot.keySet()).containsExactly(
                "title", "handle", "body", "seoTitle", "seoDescription", "published");
        assertThat(snapshot.get("handle")).isEqualTo("about-us");
        assertThat(snapshot.get("published")).isEqualTo("false");
    }

    @Test
    void updateReplacesContentAndClearsNullSeoFields() {
        Page p = page();
        p.update(new PageTitle("About"), new PageBody("<p>New</p>"), null, null);
        assertThat(p.getSeoTitle()).isEmpty();
        assertThat(p.getSeoDescription()).isEmpty();
        assertThat(p.getHandle().value()).isEqualTo("about-us"); // update never touches the handle
    }
}

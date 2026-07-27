package com.vointika.page.domain.entity;

import com.vointika.page.domain.enums.PageStatus;
import com.vointika.page.domain.valueobject.PageBody;
import com.vointika.page.domain.valueobject.PageSeoDescription;
import com.vointika.page.domain.valueobject.PageSeoTitle;
import com.vointika.page.domain.valueobject.PageTitle;
import com.vointika.shared.exception.ConflictException;
import com.vointika.shared.valueobject.Slug;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PageTest {

    private Page page() {
        return new Page(UUID.randomUUID(), UUID.randomUUID(),
                new PageTitle("About us"), new Slug("about-us"),
                new PageBody("<p>Hello</p>"),
                new PageSeoTitle("About"), new PageSeoDescription("Who we are"),
                UUID.randomUUID());
    }

    @Test
    void newPageStartsDraftAndTransitionsAreGuarded() {
        Page p = page();
        assertThat(p.getStatus()).isEqualTo(PageStatus.DRAFT);
        assertThatThrownBy(p::unpublish).isInstanceOf(ConflictException.class);
        p.publish();
        assertThat(p.getStatus()).isEqualTo(PageStatus.PUBLISHED);
        assertThatThrownBy(p::publish).isInstanceOf(ConflictException.class);
        p.unpublish();
        assertThat(p.getStatus()).isEqualTo(PageStatus.DRAFT);
    }

    @Test
    void auditSnapshotExposesExactlyTheAuditedFields() {
        Map<String, Object> snapshot = page().auditSnapshot();
        assertThat(snapshot.keySet()).containsExactly(
                "title", "handle", "body", "seoTitle", "seoDescription", "status", "templateSuffix");
        assertThat(snapshot.get("handle")).isEqualTo("about-us");
        assertThat(snapshot.get("status")).isEqualTo("DRAFT");
        assertThat(snapshot.get("templateSuffix")).isNull();
    }

    @Test
    void updateReplacesContentAndClearsNullSeoFields() {
        Page p = page();
        p.update(new PageTitle("About"), new PageBody("<p>New</p>"), null, null, "landing");
        assertThat(p.getSeoTitle()).isEmpty();
        assertThat(p.getSeoDescription()).isEmpty();
        assertThat(p.getTemplateSuffix()).isEqualTo("landing");
        assertThat(p.getHandle().value()).isEqualTo("about-us"); // update never touches the handle
    }
}

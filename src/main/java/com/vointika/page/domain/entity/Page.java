package com.vointika.page.domain.entity;

import com.vointika.page.domain.enums.PageStatus;
import com.vointika.page.domain.valueobject.PageBody;
import com.vointika.page.domain.valueobject.PageSeoDescription;
import com.vointika.page.domain.valueobject.PageSeoTitle;
import com.vointika.page.domain.valueobject.PageTitle;
import com.vointika.shared.exception.ConflictException;
import com.vointika.shared.valueobject.Handle;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * A merchant-authored CMS content page (Shopify "Page"): About / Contact /
 * FAQ / policies / meeting-point info. Content only — title, raw-HTML body,
 * SEO overrides; the page's THEME COMPOSITION will live in the (future)
 * operator theme's {@code templates/page[.suffix].json}, not here.
 *
 * <p>The {@code handle} is the page's URL segment ({@code /pages/{handle}}),
 * unique per operator and operator-chosen. It changes only through
 * {@link #rename(Handle)} — a deliberate act on its own endpoint (handle-history
 * 301s for old URLs are deferred to the storefront arc). Status moves only
 * through {@link #publish()}/{@link #unpublish()}, mirroring experiences; a
 * new page starts DRAFT.
 */
public class Page {

    private final UUID id;
    private final UUID tourOperatorId;
    private PageTitle title;
    private Handle handle;
    private PageBody body;
    private PageSeoTitle seoTitle;
    private PageSeoDescription seoDescription;
    private PageStatus status;
    private String templateSuffix;
    private final UUID createdBy;
    private final Instant createdAt;
    private Instant updatedAt;

    // Constructor for creating a brand new page (starts DRAFT)
    public Page(UUID id,
                UUID tourOperatorId,
                PageTitle title,
                Handle handle,
                PageBody body,
                PageSeoTitle seoTitle,
                PageSeoDescription seoDescription,
                UUID createdBy) {
        this.id = id;
        this.tourOperatorId = tourOperatorId;
        this.title = title;
        this.handle = handle;
        this.body = body;
        this.seoTitle = seoTitle;
        this.seoDescription = seoDescription;
        this.status = PageStatus.DRAFT;
        this.templateSuffix = null;
        this.createdBy = createdBy;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    // Constructor for reconstituting from persistence
    public Page(UUID id,
                UUID tourOperatorId,
                PageTitle title,
                Handle handle,
                PageBody body,
                PageSeoTitle seoTitle,
                PageSeoDescription seoDescription,
                PageStatus status,
                String templateSuffix,
                UUID createdBy,
                Instant createdAt,
                Instant updatedAt) {
        this.id = id;
        this.tourOperatorId = tourOperatorId;
        this.title = title;
        this.handle = handle;
        this.body = body;
        this.seoTitle = seoTitle;
        this.seoDescription = seoDescription;
        this.status = status;
        this.templateSuffix = templateSuffix;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * Whole replace of the editable content. Handle and status are
     * intentionally not touched here — the handle changes only through
     * {@link #rename(Handle)} and status moves through
     * {@link #publish()}/{@link #unpublish()}. {@code null} SEO fields /
     * template suffix clear them.
     */
    public void update(PageTitle newTitle,
                       PageBody newBody,
                       PageSeoTitle newSeoTitle,
                       PageSeoDescription newSeoDescription,
                       String newTemplateSuffix) {
        this.title = newTitle;
        this.body = newBody;
        this.seoTitle = newSeoTitle;
        this.seoDescription = newSeoDescription;
        this.templateSuffix = newTemplateSuffix;
        this.updatedAt = Instant.now();
    }

    /** DRAFT → PUBLISHED; already published → 409 (mirrors experiences). */
    public void publish() {
        if (status != PageStatus.DRAFT) {
            throw new ConflictException("Page is already published");
        }
        this.status = PageStatus.PUBLISHED;
        this.updatedAt = Instant.now();
    }

    /** PUBLISHED → DRAFT; already a draft → 409. */
    public void unpublish() {
        if (status != PageStatus.PUBLISHED) {
            throw new ConflictException("Page is not published");
        }
        this.status = PageStatus.DRAFT;
        this.updatedAt = Instant.now();
    }

    /**
     * Changes the canonical handle. Callers own the surrounding contract:
     * uniqueness among the operator's handles (409) — see
     * {@code RenamePageUseCase}.
     */
    public void rename(Handle newHandle) {
        this.handle = newHandle;
        this.updatedAt = Instant.now();
    }

    /**
     * The auditable fields as JSON-native values — the exposure guard for the
     * audit log's field diffs, shared by every audited page mutation (update,
     * rename, publish/unpublish) so each action's {@code changes} carry
     * exactly the fields it touched. {@code body} diffs whole-value.
     */
    public Map<String, Object> auditSnapshot() {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("title", title.value());
        snapshot.put("handle", handle.value());
        snapshot.put("body", body.value());
        snapshot.put("seoTitle", getSeoTitle().map(PageSeoTitle::value).orElse(null));
        snapshot.put("seoDescription", getSeoDescription().map(PageSeoDescription::value).orElse(null));
        snapshot.put("status", status.name());
        snapshot.put("templateSuffix", templateSuffix);
        return snapshot;
    }

    public UUID getId() { return id; }
    public UUID getTourOperatorId() { return tourOperatorId; }
    public PageTitle getTitle() { return title; }
    public Handle getHandle() { return handle; }
    public PageBody getBody() { return body; }
    public Optional<PageSeoTitle> getSeoTitle() { return Optional.ofNullable(seoTitle); }
    public Optional<PageSeoDescription> getSeoDescription() { return Optional.ofNullable(seoDescription); }
    public PageStatus getStatus() { return status; }
    /** Nullable — {@code null} means the base {@code page} template (mirrors experiences). */
    public String getTemplateSuffix() { return templateSuffix; }
    public UUID getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

package com.vointika.shared.port;

import java.util.List;
import java.util.UUID;

/**
 * One menu item as the storefront's navigation seam hands it over — the operator's
 * intent, not yet a URL.
 *
 * <p>Deliberately unresolved: {@code resourceId} still points at an experience or
 * page, and {@code externalUrl} is whatever the operator typed. Turning that into
 * a path is the renderer's job (and ultimately the BFF's, which owns URL shape),
 * because {@code touroperator} has no business knowing that experiences live
 * under {@code /experiences}.
 *
 * @param linkType HOME · EXPERIENCE_LIST · EXPERIENCE · PAGE · EXTERNAL_URL —
 *                 a plain string, so no context's enum crosses the seam
 */
public record NavigationItemView(
        String title,
        String linkType,
        UUID resourceId,
        String externalUrl,
        List<NavigationItemView> children) {}

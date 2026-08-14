package com.vointika.media.application.usecase;

import com.vointika.media.application.dto.output.MediaView;
import com.vointika.media.domain.entity.Media;
import com.vointika.media.domain.repository.MediaRepository;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.ListQuery;
import com.vointika.shared.list.ListSchema;
import com.vointika.shared.port.TourOperatorMembershipCheck;

import java.util.UUID;

/**
 * Lists a tour operator's media library — cursor-paginated via the shared list
 * framework, tenant-scoped. <b>Any member</b> may view; a non-member is a 404
 * ({@code ensureMember} is the defense-in-depth gate behind the interceptor).
 * Filter by {@code contentType} and {@code createdByName} (SET); sort by
 * {@code originalName}, {@code contentType}, {@code sizeBytes},
 * {@code createdByName}, {@code createdAt} (default, newest first) or {@code id}.
 *
 * <p>The uploader's name is a snapshot on the media row (frozen at upload), so
 * there is no post-pagination identity enrichment — which is exactly what makes
 * {@code createdByName} sortable/filterable off the single root (PATTERNS §6 forbids the
 * alternative join).
 */
public class ListMediaUseCase {

    public static final ListSchema SCHEMA = ListSchema.builder()
            .tenantScoped()
            .set("contentType", String.class)
            .sortable("contentType")
            .set("createdByName", String.class)
            .sortable("createdByName")
            .sortable("originalName")
            .sortable("sizeBytes")
            .instant("createdAt")
            .sortable("createdAt")
            .sortable("id")
            .defaultSort("-createdAt")
            .build();

    private final MediaRepository mediaRepository;
    private final TourOperatorMembershipCheck membershipCheck;

    public ListMediaUseCase(MediaRepository mediaRepository,
                            TourOperatorMembershipCheck membershipCheck) {
        this.mediaRepository = mediaRepository;
        this.membershipCheck = membershipCheck;
    }

    public CursorPage<MediaView> execute(ListQuery query, UUID callerUserId) {
        membershipCheck.ensureMember(callerUserId, query.tenantId());

        CursorPage<Media> page = mediaRepository.list(query);
        return new CursorPage<>(
                page.data().stream().map(MediaView::from).toList(),
                page.nextCursor());
    }
}

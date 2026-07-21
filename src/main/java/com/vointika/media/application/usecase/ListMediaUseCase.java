package com.vointika.media.application.usecase;

import com.vointika.media.application.dto.output.MediaView;
import com.vointika.media.domain.entity.Media;
import com.vointika.media.domain.repository.MediaRepository;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.ListQuery;
import com.vointika.shared.list.ListSchema;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.UserAccountQuery;
import com.vointika.shared.port.UserAccountView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Lists a tour operator's media library — cursor-paginated via the shared list
 * framework, tenant-scoped. <b>Any member</b> may view; a non-member is a 404
 * ({@code ensureMember} is the defense-in-depth gate behind the interceptor).
 * Filter by {@code contentType} (SET); sort by {@code createdAt} (default,
 * newest first) or {@code id}; page with {@code cursor}.
 *
 * <p>Each page's uploader names are enriched via ONE batched
 * {@link UserAccountQuery#findAccounts} (no N+1, best-effort — a null name never
 * drops a row).
 */
public class ListMediaUseCase {

    public static final ListSchema SCHEMA = ListSchema.builder()
            .tenantScoped()
            .set("contentType", String.class)
            .instant("createdAt")
            .sortable("createdAt")
            .sortable("id")
            .defaultSort("-createdAt")
            .build();

    private final MediaRepository mediaRepository;
    private final UserAccountQuery userAccountQuery;
    private final TourOperatorMembershipCheck membershipCheck;

    public ListMediaUseCase(MediaRepository mediaRepository,
                            UserAccountQuery userAccountQuery,
                            TourOperatorMembershipCheck membershipCheck) {
        this.mediaRepository = mediaRepository;
        this.userAccountQuery = userAccountQuery;
        this.membershipCheck = membershipCheck;
    }

    public CursorPage<MediaView> execute(ListQuery query, UUID callerUserId) {
        membershipCheck.ensureMember(callerUserId, query.tenantId());

        CursorPage<Media> page = mediaRepository.list(query);
        if (page.data().isEmpty()) {
            return new CursorPage<>(List.of(), page.nextCursor());
        }

        Set<UUID> uploaderIds = page.data().stream()
                .map(Media::getCreatedBy)
                .collect(Collectors.toSet());
        // HashMap, not toMap: a name may be null, and toMap rejects null values.
        Map<UUID, String> nameByUserId = new HashMap<>();
        for (UserAccountView account : userAccountQuery.findAccounts(uploaderIds)) {
            nameByUserId.put(account.userId(), account.name());
        }

        return new CursorPage<>(
                page.data().stream()
                        .map(media -> MediaView.from(media, nameByUserId.get(media.getCreatedBy())))
                        .toList(),
                page.nextCursor());
    }
}

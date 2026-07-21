package com.vointika.experience.application.usecase;

import com.vointika.experience.application.dto.output.ExperienceView;
import com.vointika.experience.domain.entity.Experience;
import com.vointika.experience.domain.repository.ExperienceRepository;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.ListQuery;
import com.vointika.shared.list.ListSchema;
import com.vointika.shared.media.MediaUrlBatchResolver;
import com.vointika.shared.port.TourOperatorMembershipCheck;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Lists an operator's experiences — cursor-paginated, tenant-scoped. Any member
 * may view; non-member → 404. Filter by {@code published} and/or {@code featured};
 * sort by {@code createdAt} (default, newest first), {@code name}, or {@code id}.
 *
 * <p>Media across the whole page is resolved in ONE batched lookup (no N+1) —
 * all rows share the operator.
 */
public class ListExperiencesUseCase {

    public static final ListSchema SCHEMA = ListSchema.builder()
            .tenantScoped()
            .bool("published")
            .bool("featured")
            .instant("createdAt")
            .sortable("createdAt")
            .sortable("name")
            .sortable("id")
            .defaultSort("-createdAt")
            .build();

    private final ExperienceRepository experienceRepository;
    private final MediaUrlBatchResolver mediaUrlBatchResolver;
    private final TourOperatorMembershipCheck membershipCheck;

    public ListExperiencesUseCase(ExperienceRepository experienceRepository,
                                  MediaUrlBatchResolver mediaUrlBatchResolver,
                                  TourOperatorMembershipCheck membershipCheck) {
        this.experienceRepository = experienceRepository;
        this.mediaUrlBatchResolver = mediaUrlBatchResolver;
        this.membershipCheck = membershipCheck;
    }

    public CursorPage<ExperienceView> execute(ListQuery query, UUID callerUserId) {
        membershipCheck.ensureMember(callerUserId, query.tenantId());

        CursorPage<Experience> page = experienceRepository.list(query);
        if (page.data().isEmpty()) {
            return new CursorPage<>(List.of(), page.nextCursor());
        }

        Set<UUID> allMediaIds = new HashSet<>();
        for (Experience e : page.data()) {
            allMediaIds.addAll(e.getMediaIds());
            if (e.getThumbnailMediaId() != null) {
                allMediaIds.add(e.getThumbnailMediaId());
            }
        }
        Map<UUID, String> urlsById = mediaUrlBatchResolver.resolve(query.tenantId(), allMediaIds);

        return new CursorPage<>(
                page.data().stream().map(e -> ExperienceView.from(e, urlsById)).toList(),
                page.nextCursor());
    }
}

package com.vointika.media.presentation.controller;

import com.vointika.media.application.dto.output.MediaView;
import com.vointika.media.application.usecase.DeleteMediaUseCase;
import com.vointika.media.application.usecase.GetMediaUseCase;
import com.vointika.media.application.usecase.ListMediaUseCase;
import com.vointika.media.application.usecase.UploadMediaUseCase;
import com.vointika.media.presentation.response.MediaResponse;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.ListQuery;
import com.vointika.shared.media.MediaUrlResolver;
import com.vointika.shared.web.list.CursorPageResponse;
import com.vointika.shared.web.list.ListQueryParser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.vointika.media.application.dto.input.DescribeMediaInput;
import com.vointika.media.application.usecase.DescribeMediaUseCase;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

/**
 * The operator's media library. Membership on the operator is enforced by the
 * {@code /api/tour-operators/**} interceptor (non-member → 404); the role gates
 * live in the use cases — reads are member-visible, uploads/deletes are ADMIN+.
 */
@RestController
@RequestMapping("/api/tour-operators/{tourOperatorId}/media")
public class MediaController {

    private final UploadMediaUseCase uploadMediaUseCase;
    private final DescribeMediaUseCase describeMediaUseCase;
    private final ListMediaUseCase listMediaUseCase;
    private final GetMediaUseCase getMediaUseCase;
    private final DeleteMediaUseCase deleteMediaUseCase;
    private final ListQueryParser listQueryParser;
    private final MediaUrlResolver mediaUrlResolver;

    public MediaController(UploadMediaUseCase uploadMediaUseCase,
                          DescribeMediaUseCase describeMediaUseCase,
                          ListMediaUseCase listMediaUseCase,
                          GetMediaUseCase getMediaUseCase,
                          DeleteMediaUseCase deleteMediaUseCase,
                          ListQueryParser listQueryParser,
                          MediaUrlResolver mediaUrlResolver) {
        this.uploadMediaUseCase = uploadMediaUseCase;
        this.describeMediaUseCase = describeMediaUseCase;
        this.listMediaUseCase = listMediaUseCase;
        this.getMediaUseCase = getMediaUseCase;
        this.deleteMediaUseCase = deleteMediaUseCase;
        this.listQueryParser = listQueryParser;
        this.mediaUrlResolver = mediaUrlResolver;
    }

    /** Uploads a file (multipart part {@code file}). ADMIN+; 201 + Location. */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> upload(
            @PathVariable UUID tourOperatorId,
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal UUID userId) {
        // A Supplier, not a stream: the use case reads the bytes twice — once to
        // measure the image, once to store it. The container has already spooled
        // the whole part, so a second getInputStream() re-reads the spool.
        UUID mediaId = uploadMediaUseCase.execute(
                tourOperatorId, userId,
                file.getContentType(), file.getSize(), file.getOriginalFilename(),
                () -> {
                    try {
                        return file.getInputStream();
                    } catch (IOException e) {
                        throw new InvalidFieldException(UploadMediaUseCase.UNREADABLE_UPLOAD);
                    }
                });
        return ResponseEntity
                .created(URI.create("/api/tour-operators/" + tourOperatorId + "/media/" + mediaId))
                .build();
    }

    /**
     * Sets or clears an image's alt text. ADMIN+.
     *
     * <p>Alt is the one part of a media row written after the upload: width and
     * height are measured from the bytes, but only the uploader knows what the
     * image shows. Blank clears it.
     */
    @PatchMapping("/{mediaId}")
    public ResponseEntity<Void> describe(
            @PathVariable UUID tourOperatorId,
            @PathVariable UUID mediaId,
            @RequestBody DescribeMediaInput body,
            @AuthenticationPrincipal UUID userId) {
        describeMediaUseCase.execute(tourOperatorId, mediaId, body, userId);
        return ResponseEntity.noContent().build();
    }

    /** The operator's media library — cursor-paginated. Any member; filter {@code contentType}, sort {@code createdAt}/{@code id}. */
    @GetMapping
    public ResponseEntity<CursorPageResponse<MediaResponse>> list(
            @PathVariable UUID tourOperatorId,
            @AuthenticationPrincipal UUID callerUserId,
            HttpServletRequest request) {
        ListQuery query = listQueryParser.parse(request, ListMediaUseCase.SCHEMA, tourOperatorId);
        CursorPage<MediaView> page = listMediaUseCase.execute(query, callerUserId);
        return ResponseEntity.ok(CursorPageResponse.of(page, v -> MediaResponse.from(v, mediaUrlResolver)));
    }

    /** A single media record. Any member; 404 if not under this operator. */
    @GetMapping("/{mediaId}")
    public ResponseEntity<MediaResponse> get(
            @PathVariable UUID tourOperatorId,
            @PathVariable UUID mediaId,
            @AuthenticationPrincipal UUID userId) {
        MediaView view = getMediaUseCase.execute(tourOperatorId, mediaId, userId);
        return ResponseEntity.ok(MediaResponse.from(view, mediaUrlResolver));
    }

    /** Deletes a media record (row + object). ADMIN+; 204. */
    @DeleteMapping("/{mediaId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID tourOperatorId,
            @PathVariable UUID mediaId,
            @AuthenticationPrincipal UUID userId) {
        deleteMediaUseCase.execute(tourOperatorId, mediaId, userId);
        return ResponseEntity.noContent().build();
    }
}

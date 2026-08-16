package com.vointika.media.presentation.controller;

import com.vointika.media.application.dto.output.MediaView;
import com.vointika.media.application.usecase.DeleteMediaUseCase;
import com.vointika.media.application.usecase.GetMediaUseCase;
import com.vointika.media.application.usecase.ListMediaUseCase;
import com.vointika.media.application.usecase.DescribeMediaUseCase;
import com.vointika.media.application.usecase.UploadMediaUseCase;
import com.vointika.shared.exception.ForbiddenException;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.media.MediaUrlResolver;
import com.vointika.shared.port.AccessTokenValidatorPort;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.web.list.ListQueryParser;
import com.vointika.shared.web.security.SecurityConfig;
import com.vointika.touroperator.infrastructure.web.WebConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.restdocs.payload.JsonFieldType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.multipart;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.patch;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.partWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.requestParts;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MediaController.class)
@ExtendWith(RestDocumentationExtension.class)
@Import({SecurityConfig.class, WebConfig.class, ListQueryParser.class})
class MediaControllerDocumentationTest {

    private static final String OPERATOR_ID = "019f7f33-1833-7dc1-b008-47e6c68b3ea2";
    private static final String MEDIA_ID = "aaaaaaaa-0000-4000-8000-000000000001";
    private static final String UPLOADER_ID = "bbbbbbbb-0000-4000-8000-000000000002";
    private static final String USER_ID = "550e8400-e29b-41d4-a716-446655440000";

    private MockMvc mockMvc;

    @MockitoBean private UploadMediaUseCase uploadMediaUseCase;
    @MockitoBean private DescribeMediaUseCase describeMediaUseCase;
    @MockitoBean private ListMediaUseCase listMediaUseCase;
    @MockitoBean private GetMediaUseCase getMediaUseCase;
    @MockitoBean private DeleteMediaUseCase deleteMediaUseCase;
    @MockitoBean private MediaUrlResolver mediaUrlResolver;
    @MockitoBean private TourOperatorMembershipCheck membershipCheck;
    @MockitoBean private AccessTokenValidatorPort accessTokenValidator;

    @BeforeEach
    void setUp(WebApplicationContext context, RestDocumentationContextProvider restDocumentation) {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(documentationConfiguration(restDocumentation)
                        .operationPreprocessors()
                        .withRequestDefaults(prettyPrint())
                        .withResponseDefaults(prettyPrint()))
                .apply(springSecurity())
                .build();
        when(mediaUrlResolver.toUrl(anyString()))
                .thenReturn("https://media.staging.vointika.com/tour-operators/x/img.png");
    }

    private void authenticated() {
        when(accessTokenValidator.isValid("test-access-token")).thenReturn(true);
        when(accessTokenValidator.extractUserId("test-access-token")).thenReturn(USER_ID);
    }

    private MediaView view() {
        return new MediaView(UUID.fromString(MEDIA_ID), "tour-operators/x/img.png", "image/png",
                12345, "img.png", "A catamaran at sunset", 400, 200,
                Instant.parse("2026-07-21T10:00:00Z"),
                UUID.fromString(UPLOADER_ID), "Olive Owner");
    }

    @Test
    void upload() throws Exception {
        authenticated();
        when(uploadMediaUseCase.execute(any(), any(), any(), org.mockito.ArgumentMatchers.anyLong(), any(), any()))
                .thenReturn(UUID.fromString(MEDIA_ID));

        var file = new org.springframework.mock.web.MockMultipartFile(
                "file", "img.png", "image/png", "fake-bytes".getBytes());

        mockMvc.perform(multipart("/api/tour-operators/{id}/media", OPERATOR_ID)
                        .file(file)
                        .header("Authorization", "Bearer test-access-token"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andDo(document("media/upload",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        requestParts(partWithName("file").description("The file to upload (image/* or application/pdf, ≤ 25 MB)")),
                        responseHeaders(headerWithName("Location").description("URI of the created media record"))));
    }

    @Test
    void list() throws Exception {
        authenticated();
        when(listMediaUseCase.execute(any(), any()))
                .thenReturn(new CursorPage<>(List.of(view()), "eyJ2MSI6Im5leHQifQ"));

        mockMvc.perform(get("/api/tour-operators/{id}/media", OPERATOR_ID)
                        .header("Authorization", "Bearer test-access-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].context").value("media"))
                .andExpect(jsonPath("$.data[0].url").value("https://media.staging.vointika.com/tour-operators/x/img.png"))
                .andExpect(jsonPath("$.data[0].uploadedBy.context").value("users"))
                .andExpect(jsonPath("$.nextCursor").value("eyJ2MSI6Im5leHQifQ"))
                .andDo(document("media/list",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        responseFields(
                                fieldWithPath("data[].id").description("The media id"),
                                fieldWithPath("data[].context").description("The entity's collection: \"media\""),
                                fieldWithPath("data[].url").description("Absolute URL, resolved from the stored key at read time"),
                                fieldWithPath("data[].contentType").description("MIME type"),
                                fieldWithPath("data[].sizeBytes").description("File size in bytes"),
                                fieldWithPath("data[].originalName").description("Original upload filename"),
                                fieldWithPath("data[].alt")
                                        .description("Alt text, or null until an admin writes one").optional(),
                                fieldWithPath("data[].width")
                                        .description("Pixel width, measured at upload; null for a non-image")
                                        .optional(),
                                fieldWithPath("data[].height")
                                        .description("Pixel height; null for a non-image").optional(),
                                fieldWithPath("data[].createdAt").description("When it was uploaded"),
                                fieldWithPath("data[].uploadedBy.id").description("The uploading user's id"),
                                fieldWithPath("data[].uploadedBy.context").description("The entity's collection: \"users\""),
                                fieldWithPath("data[].uploadedBy.name").description("Uploader's display name, snapshotted at upload (always present)"),
                                fieldWithPath("nextCursor").type(JsonFieldType.STRING).description("Opaque cursor for the next page; null on the last page"))));
    }

    @Test
    void getMedia() throws Exception {
        authenticated();
        when(getMediaUseCase.execute(any(), any(), any())).thenReturn(view());

        mockMvc.perform(get("/api/tour-operators/{id}/media/{mediaId}", OPERATOR_ID, MEDIA_ID)
                        .header("Authorization", "Bearer test-access-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.context").value("media"))
                .andExpect(jsonPath("$.uploadedBy.context").value("users"))
                .andDo(document("media/get",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(
                                parameterWithName("id").description("The tour operator id"),
                                parameterWithName("mediaId").description("The media id")),
                        responseFields(
                                fieldWithPath("id").description("The media id"),
                                fieldWithPath("context").description("The entity's collection: \"media\""),
                                fieldWithPath("url").description("Absolute URL, resolved from the stored key at read time"),
                                fieldWithPath("contentType").description("MIME type"),
                                fieldWithPath("sizeBytes").description("File size in bytes"),
                                fieldWithPath("originalName").description("Original upload filename"),
                                fieldWithPath("alt")
                                        .description("Alt text, or null until an admin writes one").optional(),
                                fieldWithPath("width")
                                        .description("Pixel width, measured at upload; null for a non-image")
                                        .optional(),
                                fieldWithPath("height")
                                        .description("Pixel height; null for a non-image").optional(),
                                fieldWithPath("createdAt").description("When it was uploaded"),
                                fieldWithPath("uploadedBy.id").description("The uploading user's id"),
                                fieldWithPath("uploadedBy.context").description("The entity's collection: \"users\""),
                                fieldWithPath("uploadedBy.name").description("Uploader's display name, snapshotted at upload (always present)"))));
    }

    @Test
    void deleteMedia() throws Exception {
        authenticated();

        mockMvc.perform(delete("/api/tour-operators/{id}/media/{mediaId}", OPERATOR_ID, MEDIA_ID)
                        .header("Authorization", "Bearer test-access-token"))
                .andExpect(status().isNoContent())
                .andDo(document("media/delete",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(
                                parameterWithName("id").description("The tour operator id"),
                                parameterWithName("mediaId").description("The media id"))));
    }

    @Test
    void uploadRequiresAuthentication() throws Exception {
        var file = new org.springframework.mock.web.MockMultipartFile(
                "file", "img.png", "image/png", "fake-bytes".getBytes());
        mockMvc.perform(multipart("/api/tour-operators/{id}/media", OPERATOR_ID).file(file))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void staffCannotUpload() throws Exception {
        authenticated();
        doThrow(new ForbiddenException("This action requires ADMIN privileges"))
                .when(uploadMediaUseCase).execute(any(), any(), any(),
                        org.mockito.ArgumentMatchers.anyLong(), any(), any());

        var file = new org.springframework.mock.web.MockMultipartFile(
                "file", "img.png", "image/png", "fake-bytes".getBytes());
        mockMvc.perform(multipart("/api/tour-operators/{id}/media", OPERATOR_ID)
                        .file(file)
                        .header("Authorization", "Bearer test-access-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void nonMemberGets404FromTheInterceptor() throws Exception {
        authenticated();
        doThrow(new com.vointika.shared.exception.ResourceNotFoundException("Tour operator not found"))
                .when(membershipCheck).ensureMember(eq(UUID.fromString(USER_ID)), eq(UUID.fromString(OPERATOR_ID)));

        mockMvc.perform(get("/api/tour-operators/{id}/media", OPERATOR_ID)
                        .header("Authorization", "Bearer test-access-token"))
                .andExpect(status().isNotFound());
    }

    @Test
    void describe() throws Exception {
        authenticated();

        mockMvc.perform(patch("/api/tour-operators/{id}/media/{mediaId}", OPERATOR_ID, MEDIA_ID)
                        .header("Authorization", "Bearer test-access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"alt\":\"A catamaran at sunset\"}"))
                .andExpect(status().isNoContent())
                .andDo(document("media/describe",
                        requestHeaders(headerWithName("Authorization").description("Bearer access token")),
                        pathParameters(
                                parameterWithName("id").description("The tour operator id"),
                                parameterWithName("mediaId").description(
                                        "The media id; one from another operator is a 404")),
                        requestFields(
                                fieldWithPath("alt")
                                        .description("\u2264500 chars. Blank or absent clears it. Width and "
                                                + "height are measured at upload and are not settable here")
                                        .optional())));
    }
}

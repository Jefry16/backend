package com.vointika.media.application.usecase;

import com.vointika.shared.web.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.unit.DataSize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The container's multipart ceiling and the application's own upload caps are two
 * numbers that must stay in step, and nothing made them.
 *
 * <p>{@code spring.servlet.multipart.max-file-size} was set in the first identity
 * slice, when the only upload was the 5 MB avatar, and never revisited. The
 * container spools the whole part <em>before</em> a handler runs, so a ceiling far
 * above what any endpoint accepts just buys an authenticated caller that much
 * ingest per request for a upload guaranteed to be refused.
 * {@code RequestSizeLimitFilter} exempts multipart precisely because this limit is
 * supposed to be the guard.
 */
@WebMvcTest(controllers = MultipartLimitsTest.ProbeController.class)
@Import({GlobalExceptionHandler.class, MultipartLimitsTest.ProbeController.class})
@AutoConfigureMockMvc(addFilters = false)
class MultipartLimitsTest {

    /** The largest upload any endpoint accepts (media; the avatar's cap is 5 MB). */
    private static final long LARGEST_APP_CAP = UploadMediaUseCase.MAX_BYTES;

    /** Headroom so a marginally-oversize upload still gets the app's own 422. */
    private static final long CEILING = LARGEST_APP_CAP * 2;

    @RestController
    static class ProbeController {
        @GetMapping("/probe/too-big")
        public String boom() {
            throw new MaxUploadSizeExceededException(LARGEST_APP_CAP);
        }
    }

    @Autowired private MockMvc mockMvc;

    @Test
    void theContainerCeilingStaysCloseToWhatTheApplicationActuallyAccepts() throws IOException {
        DataSize maxFile = DataSize.parse(property("spring.servlet.multipart.max-file-size"));
        DataSize maxRequest = DataSize.parse(property("spring.servlet.multipart.max-request-size"));

        assertThat(maxFile.toBytes())
                .withFailMessage(
                        "spring.servlet.multipart.max-file-size is %s but the largest upload any "
                                + "endpoint accepts is %s. The container spools the whole part before "
                                + "any handler runs, so the gap is free ingest for a request that will "
                                + "be refused. Raise the app cap or lower this.",
                        maxFile, DataSize.ofBytes(LARGEST_APP_CAP))
                .isGreaterThan(LARGEST_APP_CAP)
                .isLessThanOrEqualTo(CEILING);

        assertThat(maxRequest.toBytes())
                .withFailMessage("max-request-size (%s) must leave room for max-file-size (%s) "
                        + "plus the multipart envelope, without reopening the gap.", maxRequest, maxFile)
                .isGreaterThanOrEqualTo(maxFile.toBytes())
                .isLessThanOrEqualTo(CEILING + maxFile.toBytes());
    }

    @Test
    void anUploadOverTheContainerCeilingIsACleanPayloadTooLarge() throws Exception {
        // Lowering the ceiling only helps if tripping it is a proper answer. Spring's
        // ResponseEntityExceptionHandler maps this to 413 and GlobalExceptionHandler
        // gives it the ApiErrorResponse shape — verified, not assumed.
        mockMvc.perform(get("/probe/too-big"))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.status").value(413));
    }

    private static String property(String key) throws IOException {
        PropertySource<?> yml = new YamlPropertySourceLoader()
                .load("application.yml", new ClassPathResource("application.yml"))
                .getFirst();
        return String.valueOf(yml.getProperty(key));
    }
}

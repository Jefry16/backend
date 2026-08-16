package com.vointika.shared.web.docs;

import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;

import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;

/**
 * The application's one error body, for documentation tests to publish.
 *
 * <p><b>Every error answers this shape</b>, whichever layer produced it.
 * {@code GlobalExceptionHandler} serializes {@link
 * com.vointika.shared.web.response.ApiErrorResponse}, and {@code
 * RestAuthenticationEntryPoint} writes the same four keys by hand for the 401 it
 * raises inside the filter chain, before any controller runs. The two are
 * indistinguishable to a client, so both are documented from here.
 *
 * <p><b>{@code code} is the only conditional field.</b> It is
 * {@code @JsonInclude(NON_NULL)}, so it appears only where the throw site supplied
 * one — {@code InvalidFieldException} and {@code ConflictException} do, and most
 * others do not. That is why the descriptor carries an explicit
 * {@code type(STRING)} as well as {@code optional()}: with the field absent from a
 * payload, REST Docs cannot infer a type and fails the build rather than guessing.
 *
 * <p><b>No 401 can carry one.</b> {@code UnauthorizedException} declares only a
 * message constructor and the handler calls the two-argument {@code build}, so a
 * {@code code} on a 401 is unreachable in both paths.
 */
public final class ApiErrorSnippets {

    private ApiErrorSnippets() {
    }

    public static FieldDescriptor[] errorFields() {
        return new FieldDescriptor[]{
                fieldWithPath("status").description("The HTTP status code"),
                fieldWithPath("error").description("The status reason phrase"),
                fieldWithPath("message").description("Human-readable detail"),
                fieldWithPath("code").type(JsonFieldType.STRING)
                        .description("Machine-readable error code; absent when the throw site supplied none")
                        .optional(),
                fieldWithPath("timestamp").description("When the error was produced")
        };
    }
}

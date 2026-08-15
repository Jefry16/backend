package com.vointika.shared.web.list;

import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.list.ListQuery;
import com.vointika.shared.list.ListSchema;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A negating operator on a field declared nullable is refused at parse time.
 *
 * <p>It is refused rather than answered because the answer would be wrong and
 * would look right: {@code NOT (NULL LIKE 'x')} is UNKNOWN, {@code WHERE} keeps
 * only true rows, and every row with no value disappears from a result it
 * belongs in. A 422 says "this cannot be expressed"; the alternative is a list
 * that is quietly short.
 *
 * <p>The positive operators stay available on the same field, which is the whole
 * reason this is a per-operator refusal and not a ban on nullable filters.
 */
class NegativeFiltersOnNullableFieldsTest {

    private static final ListSchema SCHEMA = ListSchema.builder()
            .text("actorName")
            .set("actorId", UUID.class)
            .text("action")
            .nullable("actorName")
            .nullable("actorId")
            .sortable("id")
            .defaultSort("-id")
            .build();

    private final ListQueryParser parser = new ListQueryParser();

    @Test
    void refusesNeqOnANullableTextField() {
        assertThatThrownBy(() -> parse("filter[actorName][neq]", "alice"))
                .isInstanceOf(InvalidFieldException.class)
                .hasMessageContaining("actorName")
                .hasMessageContaining("may have no value");
    }

    @Test
    void refusesNotContainsOnANullableTextField() {
        assertThatThrownBy(() -> parse("filter[actorName][not_contains]", "ali"))
                .isInstanceOf(InvalidFieldException.class)
                .hasMessageContaining("not_contains");
    }

    @Test
    void refusesNotInOnANullableSetField() {
        assertThatThrownBy(() -> parse("filter[actorId][not_in]", UUID.randomUUID().toString()))
                .isInstanceOf(InvalidFieldException.class)
                .hasMessageContaining("actorId");
    }

    /** The point of refusing per operator rather than per field. */
    @Test
    void stillAllowsPositiveOperatorsOnTheSameField() {
        assertThat(parse("filter[actorName][contains]", "ali").filters().filters()).hasSize(1);
        assertThat(parse("filter[actorName][eq]", "alice").filters().filters()).hasSize(1);
        assertThat(parse("filter[actorName][starts_with]", "al").filters().filters()).hasSize(1);
    }

    /** A field that is not declared nullable keeps every operator it had. */
    @Test
    void leavesNonNullableFieldsAlone() {
        assertThat(parse("filter[action][neq]", "created").filters().filters()).hasSize(1);
    }

    @Test
    void aNullableMarkerMustNameADeclaredFilterField() {
        assertThatThrownBy(() -> ListSchema.builder()
                .text("actorName")
                .nullable("actrName")
                .sortable("id")
                .defaultSort("-id")
                .build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("names no declared filter field");
    }

    /** Sorting on a nullable column loses rows across pages, so the pair is refused outright. */
    @Test
    void aFieldCannotBeBothNullableAndSortable() {
        assertThatThrownBy(() -> ListSchema.builder()
                .text("actorName")
                .nullable("actorName")
                .sortable("actorName")
                .defaultSort("actorName")
                .build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cannot page over null sort keys");
    }

    private ListQuery parse(String key, String value) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter(key, value);
        return parser.parse(request, SCHEMA, UUID.randomUUID());
    }
}

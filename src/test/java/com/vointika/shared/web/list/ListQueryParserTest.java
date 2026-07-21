package com.vointika.shared.web.list;

import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.list.FilterOp;
import com.vointika.shared.list.ListQuery;
import com.vointika.shared.list.ListSchema;
import com.vointika.shared.list.SortDirection;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ListQueryParserTest {

    private final ListQueryParser parser = new ListQueryParser();
    private final UUID tenantId = UUID.fromString("b4e8a3c6-1f7d-4a2e-9b0c-5d8e2f1a4c9b");

    private final ListSchema schema = ListSchema.builder()
            .tenantScoped()
            .text("name")
            .set("status", TestStatus.class)
            .set("audienceId", UUID.class)
            .number("size", Long.class)
            .bool("isFeatured")
            .sortable("id")
            .sortable("name")
            .sortable("createdAt")
            .defaultSort("-id")
            .build();

    private enum TestStatus { DRAFT, PUBLISHED, ARCHIVED }

    @Test
    void appliesDefaultSortWhenNoParamsProvided() {
        MockHttpServletRequest req = new MockHttpServletRequest();

        ListQuery query = parser.parse(req, schema, tenantId);

        assertThat(query.tenantId()).isEqualTo(tenantId);
        assertThat(query.filters().filters()).isEmpty();
        assertThat(query.sort().field()).isEqualTo("id");
        assertThat(query.sort().direction()).isEqualTo(SortDirection.DESC);
        assertThat(query.cursor()).isNull();
    }

    @Test
    void parsesTextFilterWithEachOperator() {
        java.util.Set<FilterOp> textOps = java.util.Set.of(
                FilterOp.EQ, FilterOp.NEQ, FilterOp.CONTAINS,
                FilterOp.NOT_CONTAINS, FilterOp.STARTS_WITH, FilterOp.ENDS_WITH);
        for (FilterOp op : textOps) {
            MockHttpServletRequest req = new MockHttpServletRequest();
            req.setParameter("filter[name][" + op.token() + "]", "venice");

            ListQuery query = parser.parse(req, schema, tenantId);

            assertThat(query.filters().filters())
                    .singleElement()
                    .satisfies(f -> {
                        assertThat(f.field()).isEqualTo("name");
                        assertThat(f.op()).isEqualTo(op);
                        assertThat(f.value()).isEqualTo("venice");
                    });
        }
    }

    @Test
    void parsesSetFilterInWithMultipleEnumValues() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setParameter("filter[status][in]", "DRAFT,PUBLISHED");

        ListQuery query = parser.parse(req, schema, tenantId);

        assertThat(query.filters().filters())
                .singleElement()
                .satisfies(f -> {
                    assertThat(f.field()).isEqualTo("status");
                    assertThat(f.op()).isEqualTo(FilterOp.IN);
                    assertThat(f.value())
                            .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.LIST)
                            .containsExactly(TestStatus.DRAFT, TestStatus.PUBLISHED);
                });
    }

    @Test
    void parsesSetFilterNotInWithSingleValue() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setParameter("filter[status][not_in]", "ARCHIVED");

        ListQuery query = parser.parse(req, schema, tenantId);

        assertThat(query.filters().filters())
                .singleElement()
                .satisfies(f -> {
                    assertThat(f.op()).isEqualTo(FilterOp.NOT_IN);
                    assertThat(f.value())
                            .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.LIST)
                            .containsExactly(TestStatus.ARCHIVED);
                });
    }

    @Test
    void parsesSetFilterWithUuidValueType() {
        UUID a = UUID.fromString("018f8c00-0000-7000-8000-000000000001");
        UUID b = UUID.fromString("018f8c00-0000-7000-8000-000000000002");
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setParameter("filter[audienceId][in]", a + "," + b);

        ListQuery query = parser.parse(req, schema, tenantId);

        assertThat(query.filters().filters())
                .singleElement()
                .satisfies(f -> assertThat(f.value())
                        .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.LIST)
                        .containsExactly(a, b));
    }

    @Test
    void rejectsEmptySetFilter() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setParameter("filter[status][in]", "");

        assertThatThrownBy(() -> parser.parse(req, schema, tenantId))
                .isInstanceOf(InvalidFieldException.class)
                .hasMessageContaining("at least one value");
    }

    @Test
    void rejectsSetFilterWithEmptyItem() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setParameter("filter[status][in]", "DRAFT,,PUBLISHED");

        assertThatThrownBy(() -> parser.parse(req, schema, tenantId))
                .isInstanceOf(InvalidFieldException.class)
                .hasMessageContaining("empty value");
    }

    @Test
    void rejectsSetFilterExceedingMaxSize() {
        StringBuilder huge = new StringBuilder();
        for (int i = 0; i < 101; i++) {
            if (i > 0) huge.append(',');
            huge.append("DRAFT");
        }
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setParameter("filter[status][in]", huge.toString());

        assertThatThrownBy(() -> parser.parse(req, schema, tenantId))
                .isInstanceOf(InvalidFieldException.class)
                .hasMessageContaining("at most 100");
    }

    @Test
    void rejectsSetFilterWithUnparseableEnumValue() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setParameter("filter[status][in]", "BOGUS");

        assertThatThrownBy(() -> parser.parse(req, schema, tenantId))
                .isInstanceOf(InvalidFieldException.class)
                .hasMessageContaining("Invalid value 'BOGUS'");
    }

    @Test
    void rejectsTextOperatorOnSetField() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setParameter("filter[status][contains]", "DRAFT");

        assertThatThrownBy(() -> parser.parse(req, schema, tenantId))
                .isInstanceOf(InvalidFieldException.class)
                .hasMessageContaining("not allowed");
    }

    @Test
    void parsesBooleanFilterEqTrue() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setParameter("filter[isFeatured][eq]", "true");

        ListQuery query = parser.parse(req, schema, tenantId);

        assertThat(query.filters().filters())
                .singleElement()
                .satisfies(f -> {
                    assertThat(f.field()).isEqualTo("isFeatured");
                    assertThat(f.op()).isEqualTo(FilterOp.EQ);
                    assertThat(f.value()).isEqualTo(Boolean.TRUE);
                });
    }

    @Test
    void parsesBooleanFilterNeqFalse() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setParameter("filter[isFeatured][neq]", "false");

        ListQuery query = parser.parse(req, schema, tenantId);

        assertThat(query.filters().filters())
                .singleElement()
                .satisfies(f -> {
                    assertThat(f.op()).isEqualTo(FilterOp.NEQ);
                    assertThat(f.value()).isEqualTo(Boolean.FALSE);
                });
    }

    @Test
    void rejectsBooleanFilterWithNonLiteralValue() {
        // Tightening: the parser does NOT silently coerce "yes"/"1"/"FALSE"
        // into a boolean — typos would otherwise return the opposite of
        // intent without warning.
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setParameter("filter[isFeatured][eq]", "yes");

        assertThatThrownBy(() -> parser.parse(req, schema, tenantId))
                .isInstanceOf(InvalidFieldException.class)
                .hasMessageContaining("Invalid value 'yes'");
    }

    @Test
    void rejectsContainsOperatorOnBooleanField() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setParameter("filter[isFeatured][contains]", "true");

        assertThatThrownBy(() -> parser.parse(req, schema, tenantId))
                .isInstanceOf(InvalidFieldException.class)
                .hasMessageContaining("not allowed");
    }

    @Test
    void parsesAscendingSortWithoutMinusPrefix() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setParameter("sort", "name");

        ListQuery query = parser.parse(req, schema, tenantId);

        assertThat(query.sort().field()).isEqualTo("name");
        assertThat(query.sort().direction()).isEqualTo(SortDirection.ASC);
    }

    @Test
    void parsesDescendingSortWithMinusPrefix() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setParameter("sort", "-createdAt");

        ListQuery query = parser.parse(req, schema, tenantId);

        assertThat(query.sort().field()).isEqualTo("createdAt");
        assertThat(query.sort().direction()).isEqualTo(SortDirection.DESC);
    }

    @Test
    void rejectsUnknownFilterField() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setParameter("filter[bogus][eq]", "x");

        assertThatThrownBy(() -> parser.parse(req, schema, tenantId))
                .isInstanceOf(InvalidFieldException.class)
                .hasMessageContaining("Unknown filter field");
    }

    @Test
    void rejectsTextFilterWithoutOperator() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setParameter("filter[name]", "venice");

        assertThatThrownBy(() -> parser.parse(req, schema, tenantId))
                .isInstanceOf(InvalidFieldException.class)
                .hasMessageContaining("requires an operator");
    }

    @Test
    void rejectsUnknownOperator() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setParameter("filter[name][bogus]", "venice");

        assertThatThrownBy(() -> parser.parse(req, schema, tenantId))
                .isInstanceOf(InvalidFieldException.class)
                .hasMessageContaining("Unknown filter operator");
    }

    @Test
    void rejectsUnknownSortField() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setParameter("sort", "-bogus");

        assertThatThrownBy(() -> parser.parse(req, schema, tenantId))
                .isInstanceOf(InvalidFieldException.class)
                .hasMessageContaining("Unknown sort field");
    }

    @Test
    void passesCursorThroughOpaque() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setParameter("cursor", "abc123");

        ListQuery query = parser.parse(req, schema, tenantId);

        assertThat(query.cursor()).isEqualTo("abc123");
    }

    @Test
    void parsesNumberFilterWithEachScalarOperator() {
        java.util.Set<FilterOp> numberScalarOps = java.util.Set.of(
                FilterOp.EQ, FilterOp.NEQ, FilterOp.GT, FilterOp.GTE, FilterOp.LT, FilterOp.LTE);
        for (FilterOp op : numberScalarOps) {
            MockHttpServletRequest req = new MockHttpServletRequest();
            req.setParameter("filter[size][" + op.token() + "]", "42");

            ListQuery query = parser.parse(req, schema, tenantId);

            assertThat(query.filters().filters())
                    .singleElement()
                    .satisfies(f -> {
                        assertThat(f.field()).isEqualTo("size");
                        assertThat(f.op()).isEqualTo(op);
                        assertThat(f.value()).isEqualTo(42L);
                    });
        }
    }

    @Test
    void parsesBetweenFilterWithLoAndHi() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setParameter("filter[size][between]", "100,500");

        ListQuery query = parser.parse(req, schema, tenantId);

        assertThat(query.filters().filters())
                .singleElement()
                .satisfies(f -> {
                    assertThat(f.op()).isEqualTo(FilterOp.BETWEEN);
                    assertThat(f.value())
                            .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.LIST)
                            .containsExactly(100L, 500L);
                });
    }

    @Test
    void rejectsBetweenWithSingleValue() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setParameter("filter[size][between]", "100");

        assertThatThrownBy(() -> parser.parse(req, schema, tenantId))
                .isInstanceOf(InvalidFieldException.class)
                .hasMessageContaining("at least 2 values");
    }

    @Test
    void rejectsBetweenWithThreeValues() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setParameter("filter[size][between]", "100,200,300");

        assertThatThrownBy(() -> parser.parse(req, schema, tenantId))
                .isInstanceOf(InvalidFieldException.class)
                .hasMessageContaining("at most 2 values");
    }

    @Test
    void rejectsBetweenWithReversedRange() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setParameter("filter[size][between]", "500,100");

        assertThatThrownBy(() -> parser.parse(req, schema, tenantId))
                .isInstanceOf(InvalidFieldException.class)
                .hasMessageContaining("first value to be <= the second");
    }

    @Test
    void rejectsNumberFilterWithUnparseableValue() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setParameter("filter[size][gt]", "not-a-number");

        assertThatThrownBy(() -> parser.parse(req, schema, tenantId))
                .isInstanceOf(InvalidFieldException.class)
                .hasMessageContaining("Invalid value 'not-a-number'");
    }

    @Test
    void rejectsTextOperatorOnNumberField() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setParameter("filter[size][contains]", "42");

        assertThatThrownBy(() -> parser.parse(req, schema, tenantId))
                .isInstanceOf(InvalidFieldException.class)
                .hasMessageContaining("not allowed");
    }
}

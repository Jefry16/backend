package com.vointika.shared.list;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ListSchemaTest {

    @Test
    void buildsSchemaWithDefaultSortAndTextFilter() {
        ListSchema schema = ListSchema.builder()
                .tenantScoped()
                .text("name")
                .sortable("id")
                .sortable("name")
                .defaultSort("-id")
                .build();

        assertThat(schema.tenantScoped()).isTrue();
        assertThat(schema.filters()).containsKey("name");
        assertThat(schema.filters().get("name").type()).isEqualTo(FilterType.TEXT);
        assertThat(schema.sortable()).containsExactlyInAnyOrder("id", "name");
        assertThat(schema.defaultSort().field()).isEqualTo("id");
        assertThat(schema.defaultSort().direction()).isEqualTo(SortDirection.DESC);
    }

    @Test
    void rejectsDefaultSortFieldNotInSortable() {
        assertThatThrownBy(() -> ListSchema.builder()
                .sortable("id")
                .defaultSort("-name")
                .build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not in sortable");
    }

    @Test
    void rejectsBuildWithoutDefaultSort() {
        assertThatThrownBy(() -> ListSchema.builder()
                .sortable("id")
                .build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("defaultSort");
    }

    @Test
    void textFilterAllowsAllSixOperators() {
        FilterType t = FilterType.TEXT;
        assertThat(t.allowedOps()).containsExactlyInAnyOrder(
                FilterOp.EQ,
                FilterOp.NEQ,
                FilterOp.CONTAINS,
                FilterOp.NOT_CONTAINS,
                FilterOp.STARTS_WITH,
                FilterOp.ENDS_WITH
        );
    }
}

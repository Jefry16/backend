package com.vointika.shared.web.list;

import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.list.Filter;
import com.vointika.shared.list.FilterFieldDef;
import com.vointika.shared.list.FilterOp;
import com.vointika.shared.list.FilterSpec;
import com.vointika.shared.list.FilterType;
import com.vointika.shared.list.ListConstants;
import com.vointika.shared.list.ListQuery;
import com.vointika.shared.list.ListSchema;
import com.vointika.shared.list.SortDirection;
import com.vointika.shared.list.SortSpec;
import com.vointika.shared.list.ValueCoercion;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns a list endpoint's query string into a {@link ListQuery} against the
 * caller's {@link ListSchema}.
 *
 * <p><b>A parameter this does not recognize is a 422, not a shrug.</b> It used to
 * be skipped silently, which meant the API answered a misunderstood request with a
 * normal page of data and no signal at all: {@code ?limit=5} served twenty rows,
 * {@code ?sortt=-createdAt} served the default order, and the client could not tell
 * either from having been obeyed. The mistake then reads as a backend bug, because
 * the framework <em>is</em> strict about everything one level down — an unknown
 * filter field or sort field has always been a 422 — so a developer reasonably
 * concludes their parameters were validated.
 *
 * <p>There is no page-size parameter to add here: one page size for every list is a
 * decision, so a client sending one is unambiguously wrong and should hear so.
 */
@Component
public class ListQueryParser {

    private static final Pattern FILTER_KEY = Pattern.compile("^filter\\[([^]]+)](?:\\[([^]]+)])?$");

    /** Everything a list accepts that is not a {@code filter[...]} key. */
    private static final Set<String> NON_FILTER_PARAMS = Set.of("sort", "cursor");

    public ListQuery parse(HttpServletRequest request, ListSchema schema, UUID tenantId) {
        Map<String, String[]> params = request.getParameterMap();

        List<Filter> filters = new ArrayList<>();
        for (Map.Entry<String, String[]> entry : params.entrySet()) {
            String key = entry.getKey();
            Matcher m = FILTER_KEY.matcher(key);
            if (!m.matches()) {
                if (!NON_FILTER_PARAMS.contains(key)) {
                    throw new InvalidFieldException("Unknown query parameter: '" + key
                            + "'. A list accepts sort, cursor and filter[field][op].");
                }
                continue;
            }

            String field = m.group(1);
            String opToken = m.group(2);
            String value = firstValue(entry.getValue());

            FilterFieldDef def = schema.filters().get(field);
            if (def == null) {
                throw new InvalidFieldException("Unknown filter field: " + field);
            }
            if (opToken == null) {
                throw new InvalidFieldException("Filter '" + field + "' requires an operator");
            }
            FilterOp op = FilterOp.fromToken(opToken)
                    .orElseThrow(() -> new InvalidFieldException("Unknown filter operator: " + opToken));
            if (!def.type().allowedOps().contains(op)) {
                throw new InvalidFieldException(
                        "Operator '" + opToken + "' is not allowed for filter '" + field + "'");
            }
            if (value == null) {
                throw new InvalidFieldException("Filter '" + field + "' requires a value");
            }
            Object coerced = coerceValue(field, def, op, value);
            filters.add(new Filter(field, op, coerced));
        }

        SortSpec sort = parseSort(params, schema);
        String cursor = firstValue(params.get("cursor"));

        return new ListQuery(
                schema.tenantScoped() ? tenantId : null,
                new FilterSpec(filters),
                sort,
                cursor
        );
    }

    private static SortSpec parseSort(Map<String, String[]> params, ListSchema schema) {
        String token = firstValue(params.get("sort"));
        if (token == null || token.isEmpty()) {
            return schema.defaultSort();
        }
        SortSpec parsed = parseSortToken(token);
        if (!schema.sortable().contains(parsed.field())) {
            throw new InvalidFieldException("Unknown sort field: " + parsed.field());
        }
        return parsed;
    }

    private static SortSpec parseSortToken(String token) {
        if (token.charAt(0) == '-') {
            String field = token.substring(1);
            if (field.isEmpty()) {
                throw new InvalidFieldException("Invalid sort: missing field");
            }
            return new SortSpec(field, SortDirection.DESC);
        }
        return new SortSpec(token, SortDirection.ASC);
    }

    private static Object coerceValue(String field, FilterFieldDef def, FilterOp op, String raw) {
        if (def.type() == FilterType.SET) {
            return parseValueList(field, def, raw, 1, ListConstants.MAX_FILTER_SET_SIZE);
        }
        if (op == FilterOp.BETWEEN) {
            List<Object> values = parseValueList(field, def, raw, 2, 2);
            @SuppressWarnings({"unchecked", "rawtypes"})
            int cmp = ((Comparable) values.get(0)).compareTo(values.get(1));
            if (cmp > 0) {
                throw new InvalidFieldException(
                        "Filter '" + field + "' between requires the first value to be <= the second");
            }
            return values;
        }
        return ValueCoercion.coerce(raw, def.valueType());
    }

    private static List<Object> parseValueList(
            String field, FilterFieldDef def, String raw, int min, int max) {
        String[] parts = raw.split(",", -1);
        if (parts.length == 0 || (parts.length == 1 && parts[0].isEmpty())) {
            throw new InvalidFieldException("Filter '" + field + "' requires at least one value");
        }
        if (parts.length < min) {
            throw new InvalidFieldException(
                    "Filter '" + field + "' requires at least " + min + " values");
        }
        if (parts.length > max) {
            throw new InvalidFieldException(
                    "Filter '" + field + "' accepts at most " + max + " values");
        }
        List<Object> values = new ArrayList<>(parts.length);
        for (String part : parts) {
            if (part.isEmpty()) {
                throw new InvalidFieldException(
                        "Filter '" + field + "' contains an empty value");
            }
            values.add(ValueCoercion.coerce(part, def.valueType()));
        }
        return List.copyOf(values);
    }

    private static String firstValue(String[] values) {
        if (values == null || values.length == 0) return null;
        return values[0];
    }
}

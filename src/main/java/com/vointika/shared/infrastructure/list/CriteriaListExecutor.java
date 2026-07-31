package com.vointika.shared.infrastructure.list;

import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.Filter;
import com.vointika.shared.list.FilterOp;
import com.vointika.shared.list.FilterType;
import com.vointika.shared.list.ListConstants;
import com.vointika.shared.list.ListQuery;
import com.vointika.shared.list.ListSchema;
import com.vointika.shared.list.SortDirection;
import com.vointika.shared.list.SortSpec;
import com.vointika.shared.list.ValueCoercion;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Function;

@Component
public class CriteriaListExecutor {

    private static final String TENANT_FIELD = "tourOperatorId";
    private static final String ID_FIELD = "id";

    @PersistenceContext
    private EntityManager em;

    public <E, P> CursorPage<P> list(
            Class<E> entityClass,
            ListSchema schema,
            ListQuery query,
            Function<E, P> toProjection
    ) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<E> cq = cb.createQuery(entityClass);
        Root<E> root = cq.from(entityClass);

        List<Predicate> predicates = new ArrayList<>();

        if (schema.tenantScoped()) {
            predicates.add(cb.equal(root.get(TENANT_FIELD), query.tenantId()));
        }

        for (Filter f : query.filters().filters()) {
            FilterType type = schema.filters().get(f.field()).type();
            predicates.add(buildFilterPredicate(cb, root, f, type));
        }

        if (query.cursor() != null) {
            CursorCodec.Decoded c = CursorCodec.decode(query.cursor(), query.sort());
            predicates.add(buildCursorPredicate(cb, root, query.sort(), c, entityClass));
        }

        cq.where(predicates.toArray(new Predicate[0]));

        SortSpec sort = query.sort();
        Order primary = orderFor(cb, root.get(sort.field()), sort.direction());
        if (ID_FIELD.equals(sort.field())) {
            cq.orderBy(primary);
        } else {
            cq.orderBy(primary, orderFor(cb, root.get(ID_FIELD), sort.direction()));
        }

        List<E> rows = em.createQuery(cq)
                .setMaxResults(ListConstants.PAGE_SIZE + 1)
                .getResultList();

        boolean hasMore = rows.size() > ListConstants.PAGE_SIZE;
        List<E> page = hasMore ? rows.subList(0, ListConstants.PAGE_SIZE) : rows;

        String nextCursor = null;
        if (hasMore && !page.isEmpty()) {
            E last = page.getLast();
            String sortValueStr = String.valueOf(extractField(last, sort.field()));
            UUID id = (UUID) extractField(last, ID_FIELD);
            nextCursor = CursorCodec.encode(sort, sortValueStr, id);
        }

        List<P> projected = page.stream().map(toProjection).toList();
        return new CursorPage<>(projected, nextCursor);
    }

    private static Order orderFor(CriteriaBuilder cb, Path<?> path, SortDirection dir) {
        return dir == SortDirection.DESC ? cb.desc(path) : cb.asc(path);
    }

    private static Predicate buildFilterPredicate(CriteriaBuilder cb, Root<?> root, Filter f, FilterType type) {
        return switch (type) {
            case TEXT -> textPredicate(cb, root, f);
            case SET -> setPredicate(cb, root, f);
            // TIME/INSTANT reuse the comparable predicate logic — LocalTime and
            // Instant are both Comparable. Their op sets differ (TIME lacks
            // GTE/LTE) but that's enforced upstream by FilterType.allowedOps.
            case NUMBER, TIME, INSTANT -> numberPredicate(cb, root, f);
            case BOOLEAN -> booleanPredicate(cb, root, f);
        };
    }

    private static Predicate booleanPredicate(CriteriaBuilder cb, Root<?> root, Filter f) {
        Path<Boolean> path = root.get(f.field());
        Boolean value = (Boolean) f.value();
        return switch (f.op()) {
            case EQ -> cb.equal(path, value);
            case NEQ -> cb.notEqual(path, value);
            default -> throw new IllegalStateException("Unexpected boolean op: " + f.op());
        };
    }

    private static Predicate textPredicate(CriteriaBuilder cb, Root<?> root, Filter f) {
        Path<String> path = root.get(f.field());
        String value = ((String) f.value()).toLowerCase(Locale.ROOT);
        String escaped = escapeLike(value);
        Expression<String> lowerCol = cb.lower(path);
        return switch (f.op()) {
            case EQ -> cb.like(lowerCol, escaped, '\\');
            case NEQ -> cb.notLike(lowerCol, escaped, '\\');
            case CONTAINS -> cb.like(lowerCol, "%" + escaped + "%", '\\');
            case NOT_CONTAINS -> cb.notLike(lowerCol, "%" + escaped + "%", '\\');
            case STARTS_WITH -> cb.like(lowerCol, escaped + "%", '\\');
            case ENDS_WITH -> cb.like(lowerCol, "%" + escaped, '\\');
            default -> throw new IllegalStateException("Unexpected text op: " + f.op());
        };
    }

    private static Predicate setPredicate(CriteriaBuilder cb, Root<?> root, Filter f) {
        @SuppressWarnings("unchecked")
        List<Object> values = (List<Object>) f.value();
        Predicate inPredicate = root.get(f.field()).in(values);
        return f.op() == FilterOp.NOT_IN ? cb.not(inPredicate) : inPredicate;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Predicate numberPredicate(CriteriaBuilder cb, Root<?> root, Filter f) {
        Path path = root.get(f.field());
        if (f.op() == FilterOp.BETWEEN) {
            List<Object> values = (List<Object>) f.value();
            return cb.between(path, (Comparable) values.get(0), (Comparable) values.get(1));
        }
        Comparable value = (Comparable) f.value();
        return switch (f.op()) {
            case EQ -> cb.equal(path, value);
            case NEQ -> cb.notEqual(path, value);
            case GT -> cb.greaterThan(path, value);
            case GTE -> cb.greaterThanOrEqualTo(path, value);
            case LT -> cb.lessThan(path, value);
            case LTE -> cb.lessThanOrEqualTo(path, value);
            default -> throw new IllegalStateException("Unexpected number op: " + f.op());
        };
    }

    private static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Predicate buildCursorPredicate(
            CriteriaBuilder cb, Root<?> root, SortSpec sort,
            CursorCodec.Decoded cursor, Class<?> entityClass) {
        Class<?> sortFieldType = fieldType(entityClass, sort.field());
        Object sortValue = ValueCoercion.coerce(cursor.sortValue(), sortFieldType);
        UUID idValue = cursor.id();

        Path<UUID> idPath = root.get(ID_FIELD);

        if (ID_FIELD.equals(sort.field())) {
            return sort.direction() == SortDirection.DESC
                    ? cb.lessThan(idPath, idValue)
                    : cb.greaterThan(idPath, idValue);
        }

        Expression<? extends Comparable> sortExpr = root.get(sort.field());
        Comparable sortValueCmp = (Comparable) sortValue;

        Predicate primary;
        Predicate tieBreaker;
        if (sort.direction() == SortDirection.DESC) {
            primary = cb.lessThan(sortExpr, sortValueCmp);
            tieBreaker = cb.and(
                    cb.equal(root.get(sort.field()), sortValue),
                    cb.lessThan(idPath, idValue)
            );
        } else {
            primary = cb.greaterThan(sortExpr, sortValueCmp);
            tieBreaker = cb.and(
                    cb.equal(root.get(sort.field()), sortValue),
                    cb.greaterThan(idPath, idValue)
            );
        }
        return cb.or(primary, tieBreaker);
    }

    private static Class<?> fieldType(@NonNull Class<?> entityClass, String fieldName) {
        Class<?> c = entityClass;
        while (c != null && c != Object.class) {
            try {
                return c.getDeclaredField(fieldName).getType();
            } catch (NoSuchFieldException ignore) {
                c = c.getSuperclass();
            }
        }
        throw new IllegalStateException("No such field on " + entityClass.getSimpleName() + ": " + fieldName);
    }

    private static Object extractField(@NonNull Object entity, String fieldName) {
        Class<?> c = entity.getClass();
        while (c != null && c != Object.class) {
            try {
                Field f = c.getDeclaredField(fieldName);
                f.setAccessible(true);
                return f.get(entity);
            } catch (NoSuchFieldException ignore) {
                c = c.getSuperclass();
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Cannot read field " + fieldName, e);
            }
        }
        throw new IllegalStateException("No such field on " + entity.getClass().getSimpleName() + ": " + fieldName);
    }
}

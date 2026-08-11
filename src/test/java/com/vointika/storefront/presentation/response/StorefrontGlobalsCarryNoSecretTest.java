package com.vointika.storefront.presentation.response;

import org.junit.jupiter.api.Test;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Everything in this response is served to an anonymous visitor on a public
 * address, so a secret reaching it is a leak rather than a bug.
 *
 * <p>The shop row holds a <b>plaintext</b> {@code storefront_password} beside the
 * {@code password_message} that is meant to be public — one letter apart in the
 * schema, and the read that carries one could grow the other without anyone
 * noticing at review.
 *
 * <p><b>It walks the tree instead of naming fields</b>, so a component added to a
 * nested record years from now is covered by a test nobody had to remember to
 * update. That is the whole point: a list of forbidden field names would only
 * ever describe the fields that existed when it was written.
 */
class StorefrontGlobalsCarryNoSecretTest {

    /** Public by design: it is shown to a visitor at the gate. */
    private static final Set<String> ALLOWED = Set.of("passwordmessage");

    private static final Set<String> FORBIDDEN_WORDS = Set.of("password", "secret", "token", "credential");

    @Test
    void noComponentAnywhereInTheContractLooksLikeASecret() {
        Set<String> offenders = new TreeSet<>();
        Set<Class<?>> visited = new HashSet<>();
        Deque<Class<?>> queue = new ArrayDeque<>();
        queue.add(StorefrontGlobalsResponse.class);

        while (!queue.isEmpty()) {
            Class<?> type = queue.poll();
            if (!visited.add(type) || !type.isRecord()) {
                continue;
            }
            for (RecordComponent component : type.getRecordComponents()) {
                String name = component.getName().toLowerCase(Locale.ROOT);
                if (!ALLOWED.contains(name)
                        && FORBIDDEN_WORDS.stream().anyMatch(name::contains)) {
                    offenders.add(type.getSimpleName() + "." + component.getName());
                }
                queue.addAll(recordTypesIn(component.getGenericType()));
            }
        }

        assertThat(offenders)
                .withFailMessage("These reach an anonymous visitor on every storefront address:%n%s%n"
                        + "If one of them is genuinely public, say so by adding it to ALLOWED with "
                        + "the reason — do not widen the word list.", offenders)
                .isEmpty();
    }

    /**
     * The guard is worth only as much as its reach, so this proves it actually
     * descends: {@code passwordMessage} lives three levels down, on {@code Shop},
     * and a walk that stopped at the top level would never see it.
     */
    @Test
    void theWalkReachesNestedRecords() {
        Set<Class<?>> reached = new HashSet<>();
        Deque<Class<?>> queue = new ArrayDeque<>();
        queue.add(StorefrontGlobalsResponse.class);
        while (!queue.isEmpty()) {
            Class<?> type = queue.poll();
            if (!reached.add(type) || !type.isRecord()) {
                continue;
            }
            for (RecordComponent component : type.getRecordComponents()) {
                queue.addAll(recordTypesIn(component.getGenericType()));
            }
        }

        assertThat(reached).contains(
                StorefrontGlobalsResponse.Shop.class,
                StorefrontGlobalsResponse.Brand.class,
                StorefrontGlobalsResponse.Image.class,
                StorefrontGlobalsResponse.Policy.class,
                StorefrontGlobalsResponse.Language.class);
    }

    /** Unwraps {@code List<Policy>} as well as a plain {@code Policy}. */
    private static Set<Class<?>> recordTypesIn(Type type) {
        Set<Class<?>> found = new HashSet<>();
        if (type instanceof Class<?> raw && raw.isRecord()) {
            found.add(raw);
        } else if (type instanceof ParameterizedType parameterized) {
            for (Type argument : parameterized.getActualTypeArguments()) {
                found.addAll(recordTypesIn(argument));
            }
        }
        return found;
    }
}

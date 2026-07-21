package com.vointika.shared.valueobject;

import com.vointika.shared.exception.InvalidFieldException;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocaleCodeTest {

    @Test
    void acceptsAndLowercasesValidShapes() {
        assertEquals("en", new LocaleCode("en").value());
        assertEquals("es", new LocaleCode("ES").value());
        assertEquals("pt-br", new LocaleCode("pt-BR").value());
        assertEquals("zh-hans", new LocaleCode(" zh-Hans ").value());
    }

    @Test
    void rejectsBadShapesAndBlanks() {
        assertThrows(InvalidFieldException.class, () -> new LocaleCode(null));
        assertThrows(InvalidFieldException.class, () -> new LocaleCode("  "));
        assertThrows(InvalidFieldException.class, () -> new LocaleCode("english"));   // > primary subtag len
        assertThrows(InvalidFieldException.class, () -> new LocaleCode("e"));         // too short
        assertThrows(InvalidFieldException.class, () -> new LocaleCode("en_US"));     // underscore
        assertThrows(InvalidFieldException.class, () -> new LocaleCode("en-"));       // trailing dash
        assertThrows(InvalidFieldException.class, () -> new LocaleCode("toolongcode")); // > 8 chars
    }

    @Test
    void valueEqualityWorksInASet() {
        Set<LocaleCode> set = new HashSet<>();
        set.add(new LocaleCode("en"));
        set.add(new LocaleCode("EN"));
        set.add(new LocaleCode("es"));
        assertEquals(2, set.size(), "en and EN collapse to one");
        assertTrue(set.contains(new LocaleCode("es")));
    }
}

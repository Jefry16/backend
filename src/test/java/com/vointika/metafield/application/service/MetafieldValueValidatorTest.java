package com.vointika.metafield.application.service;

import com.vointika.metafield.domain.valueobject.MetafieldType;
import com.vointika.shared.exception.InvalidFieldException;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MetafieldValueValidatorTest {

    private final MetafieldValueValidator validator = new MetafieldValueValidator(new ObjectMapper());

    @Test
    void blankIsAlwaysRejected() {
        assertThatThrownBy(() -> validator.validateAndNormalize(MetafieldType.SINGLE_LINE_TEXT, "  "))
                .isInstanceOf(InvalidFieldException.class);
    }

    @Test
    void numbersBooleansAndDatesNormalize() {
        assertThat(validator.validateAndNormalize(MetafieldType.NUMBER_INTEGER, "007")).isEqualTo("7");
        assertThat(validator.validateAndNormalize(MetafieldType.NUMBER_DECIMAL, "1.50")).isEqualTo("1.50");
        assertThat(validator.validateAndNormalize(MetafieldType.BOOLEAN, "TRUE")).isEqualTo("true");
        assertThat(validator.validateAndNormalize(MetafieldType.DATE, "2026-08-01")).isEqualTo("2026-08-01");
    }

    @Test
    void typeMismatchesAre422() {
        assertThatThrownBy(() -> validator.validateAndNormalize(MetafieldType.NUMBER_INTEGER, "1.5"))
                .isInstanceOf(InvalidFieldException.class);
        assertThatThrownBy(() -> validator.validateAndNormalize(MetafieldType.BOOLEAN, "yes"))
                .isInstanceOf(InvalidFieldException.class);
        assertThatThrownBy(() -> validator.validateAndNormalize(MetafieldType.DATE, "01/08/2026"))
                .isInstanceOf(InvalidFieldException.class);
        assertThatThrownBy(() -> validator.validateAndNormalize(MetafieldType.URL, "ftp://x.test/a"))
                .isInstanceOf(InvalidFieldException.class);
        assertThatThrownBy(() -> validator.validateAndNormalize(MetafieldType.JSON, "{\"a\":1}garbage"))
                .isInstanceOf(InvalidFieldException.class);
    }

    @Test
    void decimalExponentBombIsRejectedNotMaterialized() {
        // 12 chars, but toPlainString() would expand ~2.1 billion zeros.
        assertThatThrownBy(() -> validator.validateAndNormalize(MetafieldType.NUMBER_DECIMAL, "1E2147483647"))
                .isInstanceOf(InvalidFieldException.class);
    }

    @Test
    void urlAndJsonKeepTheTrimmedOriginal() {
        assertThat(validator.validateAndNormalize(MetafieldType.URL, " https://vointika.test/a "))
                .isEqualTo("https://vointika.test/a");
        assertThat(validator.validateAndNormalize(MetafieldType.JSON, "{\"a\": 1}"))
                .isEqualTo("{\"a\": 1}");
    }
}

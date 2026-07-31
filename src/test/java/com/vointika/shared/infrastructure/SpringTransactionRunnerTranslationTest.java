package com.vointika.shared.infrastructure;

import com.vointika.shared.exception.UniqueConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.support.SQLExceptionSubclassTranslator;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * What {@link SpringTransactionRunner} does and does not translate.
 *
 * <p>Pinned because the boundary is invisible at a call site and has already been
 * got wrong: {@code DeleteMetaobjectDefinitionUseCase} caught
 * {@code UniqueConstraintViolationException} to turn a foreign-key violation into
 * a 409, and that catch could never fire — the delete 500'd instead. A use case
 * that needs to answer for a non-unique constraint must ask the DB first; it
 * cannot catch its way there.
 */
class SpringTransactionRunnerTranslationTest {

    private static final PlatformTransactionManager PASS_THROUGH = new PlatformTransactionManager() {
        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) {
            return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status) { }

        @Override
        public void rollback(TransactionStatus status) { }
    };

    private final SpringTransactionRunner runner = new SpringTransactionRunner(PASS_THROUGH);

    @Test
    void postgresMapsAUniqueViolationToDuplicateKeyAndEverythingElseToItsParent() {
        var translator = new SQLExceptionSubclassTranslator();

        // 23505 unique_violation — the one race the runner is allowed to absorb.
        assertThat(translator.translate("t", "s", new SQLException("dup", "23505")))
                .isInstanceOf(DuplicateKeyException.class);

        // 23503 foreign_key, 23502 not_null, 23514 check — defects, not races.
        for (String sqlState : new String[] {"23503", "23502", "23514"}) {
            assertThat(translator.translate("t", "s", new SQLException("boom", sqlState)))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .isNotInstanceOf(DuplicateKeyException.class);
        }
    }

    @Test
    void aDuplicateKeyBecomesTheApplicationLayersUniqueConstraintViolation() {
        assertThatThrownBy(() -> runner.run(() -> {
            throw new DuplicateKeyException("users_email_unique");
        })).isInstanceOf(UniqueConstraintViolationException.class);

        assertThatThrownBy(() -> runner.call(() -> {
            throw new DuplicateKeyException("users_email_unique");
        })).isInstanceOf(UniqueConstraintViolationException.class);
    }

    @Test
    void aForeignKeyViolationPropagatesUntranslated() {
        assertThatThrownBy(() -> runner.run(() -> {
            throw new DataIntegrityViolationException("fk_metaobject_definition");
        }))
                .isInstanceOf(DataIntegrityViolationException.class)
                .isNotInstanceOf(UniqueConstraintViolationException.class);
    }
}

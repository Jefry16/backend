package com.vointika.shared.infrastructure;

import com.vointika.shared.exception.UniqueConstraintViolationException;
import com.vointika.shared.port.TransactionRunner;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

/**
 * The one place a persistence exception is translated.
 *
 * <p>A lost race against a unique constraint surfaces <em>here</em>, not at
 * {@code repository.save()}: JPA flushes at commit, by which point the repository
 * call has long returned. That is what makes this adapter the seam — translating
 * inside the repository implementations would have missed it entirely.
 *
 * <p>Only {@link DuplicateKeyException} is translated. Its parent,
 * {@code DataIntegrityViolationException}, also covers foreign-key, not-null and
 * check-constraint failures — defects, not races — which now propagate and become
 * the 500 they are. Catching the parent, as 21 use cases used to, meant a genuine
 * bug could be handled as "someone else got there first".
 */
@Component
public class SpringTransactionRunner implements TransactionRunner {

    private final TransactionTemplate tx;

    public SpringTransactionRunner(PlatformTransactionManager transactionManager) {
        this.tx = new TransactionTemplate(transactionManager);
    }

    @Override
    public <T> T call(Supplier<T> work) {
        try {
            return tx.execute(status -> work.get());
        } catch (DuplicateKeyException e) {
            throw translate(e);
        }
    }

    @Override
    public void run(Runnable work) {
        try {
            tx.executeWithoutResult(status -> work.run());
        } catch (DuplicateKeyException e) {
            throw translate(e);
        }
    }

    private static UniqueConstraintViolationException translate(DuplicateKeyException e) {
        return new UniqueConstraintViolationException(
                "A concurrent write already created this record", e);
    }
}

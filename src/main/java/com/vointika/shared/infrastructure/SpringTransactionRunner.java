package com.vointika.shared.infrastructure;

import com.vointika.shared.port.TransactionRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

@Component
public class SpringTransactionRunner implements TransactionRunner {

    private final TransactionTemplate tx;

    public SpringTransactionRunner(PlatformTransactionManager transactionManager) {
        this.tx = new TransactionTemplate(transactionManager);
    }

    @Override
    public <T> T call(Supplier<T> work) {
        return tx.execute(status -> work.get());
    }

    @Override
    public void run(Runnable work) {
        tx.executeWithoutResult(status -> work.run());
    }
}

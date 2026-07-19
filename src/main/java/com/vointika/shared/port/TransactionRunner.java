package com.vointika.shared.port;

import java.util.function.Supplier;

public interface TransactionRunner {

    <T> T call(Supplier<T> work);

    void run(Runnable work);
}

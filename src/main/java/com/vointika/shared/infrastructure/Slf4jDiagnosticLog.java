package com.vointika.shared.infrastructure;

import com.vointika.shared.port.DiagnosticLogPort;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class Slf4jDiagnosticLog implements DiagnosticLogPort {

    @Override
    public void warn(Class<?> source, String message, Object... args) {
        LoggerFactory.getLogger(source).warn(message, args);
    }

    @Override
    public void info(Class<?> source, String message, Object... args) {
        LoggerFactory.getLogger(source).info(message, args);
    }
}

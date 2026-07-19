package com.vointika.shared.service;

import com.github.f4b6a3.uuid.UuidCreator;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class UuidV7IdGenerator implements IdGenerator {

    @Override
    public UUID newId() {
        return UuidCreator.getTimeOrderedEpoch();
    }
}

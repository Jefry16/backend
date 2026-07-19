package com.vointika.reference.application.usecase;

import com.vointika.reference.domain.entity.Timezone;
import com.vointika.reference.domain.repository.TimezoneRepository;

import java.util.List;

public class ListTimezonesUseCase {

    private final TimezoneRepository timezoneRepository;

    public ListTimezonesUseCase(TimezoneRepository timezoneRepository) {
        this.timezoneRepository = timezoneRepository;
    }

    public List<Timezone> execute() {
        return timezoneRepository.findAll();
    }
}

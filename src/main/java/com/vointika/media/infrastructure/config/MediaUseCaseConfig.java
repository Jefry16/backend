package com.vointika.media.infrastructure.config;

import com.vointika.media.application.port.MediaStoragePort;
import com.vointika.media.application.usecase.DeleteMediaUseCase;
import com.vointika.media.application.usecase.GetMediaUseCase;
import com.vointika.media.application.usecase.ListMediaUseCase;
import com.vointika.media.application.usecase.UploadMediaUseCase;
import com.vointika.media.domain.repository.MediaRepository;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.UserAccountQuery;
import com.vointika.shared.service.IdGenerator;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration("mediaUseCaseConfig")
@EnableConfigurationProperties(MediaS3Properties.class)
public class MediaUseCaseConfig {

    @Bean
    public UploadMediaUseCase uploadMediaUseCase(
            MediaRepository mediaRepository,
            MediaStoragePort mediaStoragePort,
            TourOperatorMembershipCheck membershipCheck,
            IdGenerator idGenerator) {
        return new UploadMediaUseCase(mediaRepository, mediaStoragePort, membershipCheck, idGenerator);
    }

    @Bean
    public ListMediaUseCase listMediaUseCase(
            MediaRepository mediaRepository,
            UserAccountQuery userAccountQuery,
            TourOperatorMembershipCheck membershipCheck) {
        return new ListMediaUseCase(mediaRepository, userAccountQuery, membershipCheck);
    }

    @Bean
    public GetMediaUseCase getMediaUseCase(
            MediaRepository mediaRepository,
            UserAccountQuery userAccountQuery,
            TourOperatorMembershipCheck membershipCheck) {
        return new GetMediaUseCase(mediaRepository, userAccountQuery, membershipCheck);
    }

    @Bean
    public DeleteMediaUseCase deleteMediaUseCase(
            MediaRepository mediaRepository,
            MediaStoragePort mediaStoragePort,
            TourOperatorMembershipCheck membershipCheck) {
        return new DeleteMediaUseCase(mediaRepository, mediaStoragePort, membershipCheck);
    }
}

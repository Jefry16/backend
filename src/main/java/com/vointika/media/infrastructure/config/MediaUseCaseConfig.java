package com.vointika.media.infrastructure.config;

import com.vointika.media.application.port.MediaStoragePort;
import com.vointika.media.application.usecase.DeleteMediaUseCase;
import com.vointika.media.application.usecase.GetMediaUseCase;
import com.vointika.media.application.usecase.ListMediaUseCase;
import com.vointika.media.application.port.ImageDimensionsPort;
import com.vointika.media.application.usecase.DescribeMediaUseCase;
import com.vointika.media.application.usecase.UploadMediaUseCase;
import com.vointika.media.domain.repository.MediaRepository;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
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
            ImageDimensionsPort imageDimensionsPort,
            TourOperatorMembershipCheck membershipCheck,
            UserAccountQuery userAccountQuery,
            IdGenerator idGenerator,
            TransactionRunner transactionRunner,
            AuditTrailPort auditTrailPort) {
        return new UploadMediaUseCase(
                mediaRepository, mediaStoragePort, imageDimensionsPort, membershipCheck, userAccountQuery, idGenerator,
                transactionRunner, auditTrailPort);
    }

    @Bean
    public ListMediaUseCase listMediaUseCase(
            MediaRepository mediaRepository,
            TourOperatorMembershipCheck membershipCheck) {
        return new ListMediaUseCase(mediaRepository, membershipCheck);
    }

    @Bean
    public GetMediaUseCase getMediaUseCase(
            MediaRepository mediaRepository,
            TourOperatorMembershipCheck membershipCheck) {
        return new GetMediaUseCase(mediaRepository, membershipCheck);
    }

    @Bean
    public DeleteMediaUseCase deleteMediaUseCase(
            MediaRepository mediaRepository,
            MediaStoragePort mediaStoragePort,
            TourOperatorMembershipCheck membershipCheck,
            TransactionRunner transactionRunner,
            AuditTrailPort auditTrailPort) {
        return new DeleteMediaUseCase(mediaRepository, mediaStoragePort, membershipCheck,
                transactionRunner, auditTrailPort);
    }

    @Bean
    public DescribeMediaUseCase describeMediaUseCase(
            MediaRepository mediaRepository,
            TourOperatorMembershipCheck membershipCheck,
            TransactionRunner transactionRunner,
            AuditTrailPort auditTrailPort) {
        return new DescribeMediaUseCase(mediaRepository, membershipCheck,
                transactionRunner, auditTrailPort);
    }
}

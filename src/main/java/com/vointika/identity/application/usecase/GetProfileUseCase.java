package com.vointika.identity.application.usecase;

import com.vointika.identity.application.dto.input.GetProfileInput;
import com.vointika.identity.application.dto.output.GetProfileOutput;
import com.vointika.identity.domain.entity.User;
import com.vointika.identity.domain.repository.UserRepository;
import com.vointika.shared.port.UserTourOperatorMembershipsQuery;


public class GetProfileUseCase {

    private final UserRepository userRepository;
    private final UserTourOperatorMembershipsQuery membershipsQuery;

    public GetProfileUseCase(UserRepository userRepository,
                             UserTourOperatorMembershipsQuery membershipsQuery) {
        this.userRepository = userRepository;
        this.membershipsQuery = membershipsQuery;
    }

    public GetProfileOutput execute(GetProfileInput input) {
        User user = userRepository.requireById(input.userId());
        return new GetProfileOutput(
                user.getId(),
                user.getName().value(),
                user.getAvatarKey(),
                user.getLanguage(),
                membershipsQuery.findForUser(user.getId())
        );
    }
}

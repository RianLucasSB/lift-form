package com.rianlucassb.liftform.core.usecases.user.getcurrentuser;

import com.rianlucassb.liftform.core.domain.exception.EntityNotFoundException;
import com.rianlucassb.liftform.core.domain.model.User;
import com.rianlucassb.liftform.core.gateway.user.UserRepository;

import java.util.UUID;

public class GetCurrentUserUseCaseImpl implements GetCurrentUserUseCase {

    private final UserRepository userRepository;

    public GetCurrentUserUseCaseImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public GetCurrentUserUseCaseOutput execute(GetCurrentUserUseCaseInput input) {
        User user = userRepository.findById(UUID.fromString(input.userId()))
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        return new GetCurrentUserUseCaseOutput(user.username(), user.email());
    }
}

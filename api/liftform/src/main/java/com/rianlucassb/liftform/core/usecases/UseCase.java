package com.rianlucassb.liftform.core.usecases;

public interface UseCase<Input, Output> {
    Output execute(Input input);
}

package com.rianlucassb.liftform.core.domain.model.enums;

public enum ExerciseType {
    SQUAT;

    public static ExerciseType from(String value){
        try {
            return ExerciseType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid exercise type: " + value);
        }
    }
}

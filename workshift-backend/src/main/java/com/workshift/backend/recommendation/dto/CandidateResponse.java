package com.workshift.backend.recommendation.dto;

public record CandidateResponse(
        Long userId,
        String fullName,
        String username
) {
}

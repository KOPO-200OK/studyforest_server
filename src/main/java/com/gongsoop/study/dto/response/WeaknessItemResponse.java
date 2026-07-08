package com.gongsoop.study.dto.response;

public record WeaknessItemResponse(
        String era,
        String category,
        Long solvedCount,
        Long correctCount,
        Long wrongCount,
        Double accuracyRate
) {
}
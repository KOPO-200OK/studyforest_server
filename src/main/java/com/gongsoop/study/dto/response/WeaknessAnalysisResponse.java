package com.gongsoop.study.dto.response;

import java.util.List;

public record WeaknessAnalysisResponse(
        List<WeaknessItemResponse> items
) {
}
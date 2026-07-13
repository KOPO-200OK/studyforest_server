package com.gongsoop.ai.util;

import java.util.List;
import java.util.Locale;

public final class AiChatIntentClassifier {

    private static final List<String> MOTIVATION_KEYWORDS =
            List.of(
                    "동기부여",
                    "응원",
                    "격려",
                    "힘들어",
                    "힘들다",
                    "지쳤어",
                    "지친다",
                    "포기하고싶",
                    "포기할까",
                    "공부하기싫",
                    "하기싫어",
                    "의욕이없",
                    "집중이안",
                    "집중안돼",
                    "불안해",
                    "자신감이없",
                    "슬럼프",
                    "멘탈",
                    "시험이무서",
                    "떨어질까",
                    "합격할수있",
                    "할수있을까"
            );

    private AiChatIntentClassifier() {
    }

    /**
     * 사용자의 메시지가 시험 공부 동기부여나
     * 간단한 일상적인 격려 요청인지 확인합니다.
     */
    public static boolean isMotivationRequest(
            String message
    ) {
        if (
                message == null
                        || message.isBlank()
        ) {
            return false;
        }

        String normalized =
                message
                        .toLowerCase(
                                Locale.ROOT
                        )
                        .replaceAll(
                                "\\s+",
                                ""
                        );

        return MOTIVATION_KEYWORDS
                .stream()
                .anyMatch(
                        normalized::contains
                );
    }
}
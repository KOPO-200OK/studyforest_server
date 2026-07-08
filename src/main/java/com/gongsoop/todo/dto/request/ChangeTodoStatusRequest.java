package com.gongsoop.todo.dto.request;

import jakarta.validation.constraints.NotNull;

public record ChangeTodoStatusRequest(
        @NotNull(message = "완료 여부를 입력해주세요")
        Boolean isDone
) {
}

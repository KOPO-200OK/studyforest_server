package com.gongsoop.todo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateTodoRequest(
        @NotBlank(message = "할 일 내용을 입력해주세요")
        @Size(max = 500, message = "할 일은 500자 이하로 입력해주세요")
        String content,

        LocalDate todoDate
) {
}

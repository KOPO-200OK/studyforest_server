package com.gongsoop.todo.dto.response;

import com.gongsoop.todo.entity.DailyTodo;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TodoResponse(
        Long todoId,
        String content,
        boolean isDone,
        LocalDate todoDate,
        LocalDateTime createdAt
) {

    public static TodoResponse from(DailyTodo todo) {
        return new TodoResponse(
                todo.getTodoId(),
                todo.getContent(),
                todo.isDone(),
                todo.getTodoDate(),
                todo.getCreatedAt()
        );
    }
}

package com.gongsoop.todo.controller;

import com.gongsoop.global.response.ApiResponse;
import com.gongsoop.todo.dto.request.ChangeTodoStatusRequest;
import com.gongsoop.todo.dto.request.CreateTodoRequest;
import com.gongsoop.todo.dto.response.TodoResponse;
import com.gongsoop.todo.service.TodoService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/todos")
public class TodoController {

    private final TodoService todoService;

    public TodoController(TodoService todoService) {
        this.todoService = todoService;
    }

    @GetMapping
    public ApiResponse<List<TodoResponse>> getTodos(
            @AuthenticationPrincipal String email,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ApiResponse.success(
                "오늘 할 일을 조회했습니다",
                todoService.getTodos(email, date)
        );
    }

    @PostMapping
    public ApiResponse<TodoResponse> createTodo(
            @Valid @RequestBody CreateTodoRequest request,
            @AuthenticationPrincipal String email
    ) {
        return ApiResponse.success(
                "할 일을 추가했습니다",
                todoService.createTodo(request, email)
        );
    }

    @PatchMapping("/{todoId}/status")
    public ApiResponse<TodoResponse> changeTodoStatus(
            @PathVariable Long todoId,
            @Valid @RequestBody ChangeTodoStatusRequest request,
            @AuthenticationPrincipal String email
    ) {
        return ApiResponse.success(
                "할 일 상태를 변경했습니다",
                todoService.changeTodoStatus(todoId, request, email)
        );
    }

    @DeleteMapping("/{todoId}")
    public ApiResponse<Void> deleteTodo(
            @PathVariable Long todoId,
            @AuthenticationPrincipal String email
    ) {
        todoService.deleteTodo(todoId, email);

        return ApiResponse.success("할 일을 삭제했습니다");
    }
}

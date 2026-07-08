package com.gongsoop.todo.service;

import com.gongsoop.global.exception.BusinessException;
import com.gongsoop.member.entity.Member;
import com.gongsoop.member.repository.MemberRepository;
import com.gongsoop.todo.dto.request.ChangeTodoStatusRequest;
import com.gongsoop.todo.dto.request.CreateTodoRequest;
import com.gongsoop.todo.dto.response.TodoResponse;
import com.gongsoop.todo.entity.DailyTodo;
import com.gongsoop.todo.repository.TodoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class TodoService {

    private final TodoRepository todoRepository;
    private final MemberRepository memberRepository;

    public TodoService(TodoRepository todoRepository, MemberRepository memberRepository) {
        this.todoRepository = todoRepository;
        this.memberRepository = memberRepository;
    }

    public List<TodoResponse> getTodos(String email, LocalDate date) {
        Long memberId = getCurrentMemberId(email);
        LocalDate targetDate = date != null ? date : LocalDate.now();

        return todoRepository.findByMemberIdAndTodoDate(memberId, targetDate)
                .stream()
                .map(TodoResponse::from)
                .toList();
    }

    @Transactional
    public TodoResponse createTodo(CreateTodoRequest request, String email) {
        Long memberId = getCurrentMemberId(email);
        LocalDate todoDate = request.todoDate() != null ? request.todoDate() : LocalDate.now();

        DailyTodo todo = todoRepository.save(
                DailyTodo.create(memberId, request.content().trim(), todoDate)
        );

        return TodoResponse.from(todo);
    }

    @Transactional
    public TodoResponse changeTodoStatus(Long todoId, ChangeTodoStatusRequest request, String email) {
        Long memberId = getCurrentMemberId(email);
        DailyTodo todo = getOwnedTodo(todoId, memberId);

        todo.changeStatus(request.isDone());

        return TodoResponse.from(todo);
    }

    @Transactional
    public void deleteTodo(Long todoId, String email) {
        Long memberId = getCurrentMemberId(email);
        DailyTodo todo = getOwnedTodo(todoId, memberId);

        todoRepository.delete(todo);
    }

    private DailyTodo getOwnedTodo(Long todoId, Long memberId) {
        return todoRepository.findByTodoIdAndMemberId(todoId, memberId)
                .orElseThrow(() -> new BusinessException(
                        "TODO_NOT_FOUND",
                        "할 일을 찾을 수 없습니다",
                        HttpStatus.NOT_FOUND
                ));
    }

    private Long getCurrentMemberId(String email) {
        if (email == null || email.isBlank()) {
            throw new BusinessException(
                    "UNAUTHORIZED",
                    "로그인이 필요합니다",
                    HttpStatus.UNAUTHORIZED
            );
        }

        Member member = memberRepository.findByEmail(email)
                .filter(m -> !m.isDeleted())
                .orElseThrow(() -> new BusinessException(
                        "MEMBER_NOT_FOUND",
                        "회원 정보를 찾을 수 없습니다",
                        HttpStatus.NOT_FOUND
                ));

        return member.getId();
    }
}

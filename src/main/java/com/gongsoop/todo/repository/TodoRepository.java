package com.gongsoop.todo.repository;

import com.gongsoop.todo.entity.DailyTodo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TodoRepository extends JpaRepository<DailyTodo, Long> {

    @Query("""
            SELECT t
            FROM DailyTodo t
            WHERE t.memberId = :memberId
              AND t.todoDate = :todoDate
            ORDER BY t.createdAt ASC, t.todoId ASC
            """)
    List<DailyTodo> findByMemberIdAndTodoDate(
            @Param("memberId") Long memberId,
            @Param("todoDate") LocalDate todoDate
    );

    @Query("""
            SELECT t
            FROM DailyTodo t
            WHERE t.todoId = :todoId
              AND t.memberId = :memberId
            """)
    Optional<DailyTodo> findByTodoIdAndMemberId(
            @Param("todoId") Long todoId,
            @Param("memberId") Long memberId
    );
}

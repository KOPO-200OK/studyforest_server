package com.gongsoop.todo.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "DAILY_TODO")
public class DailyTodo {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "daily_todo_seq")
    @SequenceGenerator(
            name = "daily_todo_seq",
            sequenceName = "SEQ_DAILY_TODO",
            allocationSize = 1
    )
    @Column(name = "DAILY_TODO_ID")
    private Long todoId;

    @Column(name = "USER_ID", nullable = false)
    private Long memberId;

    @Column(name = "CONTENT", nullable = false, length = 500)
    private String content;

    @Column(name = "IS_DONE", nullable = false, length = 1)
    private String isDone;

    @Column(name = "TODO_DATE", nullable = false)
    private LocalDate todoDate;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT", nullable = false)
    private LocalDateTime updatedAt;

    protected DailyTodo() {
    }

    private DailyTodo(Long memberId, String content, LocalDate todoDate) {
        this.memberId = memberId;
        this.content = content;
        this.todoDate = todoDate;
        this.isDone = "N";
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public static DailyTodo create(Long memberId, String content, LocalDate todoDate) {
        return new DailyTodo(memberId, content, todoDate);
    }

    public void changeContent(String content) {
        this.content = content;
        this.updatedAt = LocalDateTime.now();
    }

    public void changeStatus(boolean done) {
        this.isDone = done ? "Y" : "N";
        this.updatedAt = LocalDateTime.now();
    }

    public Long getTodoId() {
        return todoId;
    }

    public Long getMemberId() {
        return memberId;
    }

    public String getContent() {
        return content;
    }

    public boolean isDone() {
        return "Y".equals(isDone);
    }

    public LocalDate getTodoDate() {
        return todoDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}

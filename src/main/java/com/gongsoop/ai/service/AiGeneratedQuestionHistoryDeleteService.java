package com.gongsoop.ai.service;

import com.gongsoop.ai.entity.AiGeneratedQuestion;
import com.gongsoop.ai.entity.AiGeneratedQuestionSet;
import com.gongsoop.ai.repository.AiGeneratedQuestionRepository;
import com.gongsoop.ai.repository.AiGeneratedQuestionSetRepository;
import com.gongsoop.ai.repository.AiGeneratedQuestionSolveRecordRepository;
import com.gongsoop.global.exception.BusinessException;
import com.gongsoop.member.entity.Member;
import com.gongsoop.member.repository.MemberRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AiGeneratedQuestionHistoryDeleteService {

    private final MemberRepository memberRepository;

    private final AiGeneratedQuestionSetRepository
            questionSetRepository;

    private final AiGeneratedQuestionRepository
            questionRepository;

    private final AiGeneratedQuestionSolveRecordRepository
            solveRecordRepository;

    public AiGeneratedQuestionHistoryDeleteService(
            MemberRepository memberRepository,
            AiGeneratedQuestionSetRepository questionSetRepository,
            AiGeneratedQuestionRepository questionRepository,
            AiGeneratedQuestionSolveRecordRepository solveRecordRepository
    ) {
        this.memberRepository =
                memberRepository;

        this.questionSetRepository =
                questionSetRepository;

        this.questionRepository =
                questionRepository;

        this.solveRecordRepository =
                solveRecordRepository;
    }

    /**
     * AI 문제 세트를 삭제합니다.
     *
     * 삭제 순서:
     * 1. 해당 문제의 풀이 기록
     * 2. 생성된 문제
     * 3. 생성 문제 세트
     */
    @Transactional
    public void deleteGeneratedQuestionSet(
            String email,
            Long setId
    ) {
        Long memberId =
                getCurrentMemberId(
                        email
                );

        AiGeneratedQuestionSet questionSet =
                questionSetRepository
                        .findByAiGeneratedQuestionSetIdAndMemberId(
                                setId,
                                memberId
                        )
                        .orElseThrow(() ->
                                new BusinessException(
                                        "AI_GENERATED_QUESTION_SET_NOT_FOUND",
                                        "AI 생성 문제 기록을 찾을 수 없습니다",
                                        HttpStatus.NOT_FOUND
                                )
                        );

        List<AiGeneratedQuestion> questions =
                questionRepository
                        .findByAiGeneratedQuestionSetIdOrderByQuestionOrderAsc(
                                setId
                        );

        List<Long> questionIds =
                questions.stream()
                        .map(
                                AiGeneratedQuestion
                                        ::getAiGeneratedQuestionId
                        )
                        .toList();

        if (!questionIds.isEmpty()) {
            solveRecordRepository
                    .deleteByAiGeneratedQuestionIdIn(
                            questionIds
                    );
        }

        questionRepository
                .deleteByAiGeneratedQuestionSetId(
                        setId
                );

        questionSetRepository.delete(
                questionSet
        );
    }

    private Long getCurrentMemberId(
            String email
    ) {
        if (
                email == null
                        || email.isBlank()
        ) {
            throw new BusinessException(
                    "UNAUTHORIZED",
                    "로그인이 필요합니다",
                    HttpStatus.UNAUTHORIZED
            );
        }

        Member member =
                memberRepository
                        .findByEmail(
                                email
                        )
                        .filter(
                                foundMember ->
                                        !foundMember.isDeleted()
                        )
                        .orElseThrow(() ->
                                new BusinessException(
                                        "MEMBER_NOT_FOUND",
                                        "회원 정보를 찾을 수 없습니다",
                                        HttpStatus.UNAUTHORIZED
                                )
                        );

        return member.getId();
    }
}
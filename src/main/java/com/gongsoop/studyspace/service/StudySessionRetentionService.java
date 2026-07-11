package com.gongsoop.studyspace.service;

import com.gongsoop.studyspace.entity.StudySessionStatus;
import com.gongsoop.studyspace.repository.StudySessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

/**
 * "이번 주" 학습 시간 통계는 매주 월요일 초기화된다. 지난 주 이전에 종료된 세션은
 * 더 이상 화면에 노출될 일이 없으므로 매주 월요일 새벽에 삭제해 테이블을 가볍게 유지한다.
 * cutoff를 매 실행 시점 기준 "이번 주 월요일"로 다시 계산하므로, 스케줄이 밀리거나
 * 재기동 직후 실행돼도 진행 중이거나 이번 주에 속한 세션은 절대 지우지 않는다.
 */
@Service
public class StudySessionRetentionService {

    private static final Logger log = LoggerFactory.getLogger(StudySessionRetentionService.class);

    private static final List<StudySessionStatus> TERMINAL_STATUSES = List.of(
            StudySessionStatus.COMPLETED,
            StudySessionStatus.AUTO_TERMINATED,
            StudySessionStatus.FORCED_TERMINATED
    );

    private final StudySessionRepository studySessionRepository;
    private final Clock clock;

    @Autowired
    public StudySessionRetentionService(StudySessionRepository studySessionRepository) {
        this(studySessionRepository, Clock.systemDefaultZone());
    }

    StudySessionRetentionService(StudySessionRepository studySessionRepository, Clock clock) {
        this.studySessionRepository = studySessionRepository;
        this.clock = clock;
    }

    @Scheduled(cron = "${studyspace.retention.cleanup-cron:0 30 3 * * MON}")
    @Transactional
    public void purgePreviousWeeks() {
        LocalDate weekStart = LocalDate.now(clock).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        int deleted = studySessionRepository.deleteByStudyDateBeforeAndStatusIn(weekStart, TERMINAL_STATUSES);
        log.info("지난 주 학습 세션 정리 완료: cutoff={}, deleted={}", weekStart, deleted);
    }
}

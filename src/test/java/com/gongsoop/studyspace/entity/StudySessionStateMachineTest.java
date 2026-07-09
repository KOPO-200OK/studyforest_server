package com.gongsoop.studyspace.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 학습 세션 상태머신(일시정지·연결중단·재접속·자동종료)의 시간 계산과 idempotency 검증.
 * 인프라 없이 순수 도메인 로직만 테스트한다.
 */
class StudySessionStateMachineTest {

    private static final LocalDateTime T0 = LocalDateTime.of(2026, 7, 9, 10, 0, 0);

    private StudySession runningSessionStartedAt(LocalDateTime start) {
        return StudySession.start(null, null, null, "한국사", start);
    }

    @Test
    void disconnectAccumulatesOnlyUpToLastSeenAndPreservesResumeStatus() {
        StudySession session = runningSessionStartedAt(T0);

        // 마지막 확인 시각(last_seen)까지만 인정
        session.disconnect(T0.plusSeconds(100));

        assertThat(session.getStatus()).isEqualTo(StudySessionStatus.DISCONNECTED);
        assertThat(session.getResumeStatus()).isEqualTo(StudySessionStatus.RUNNING);
        // DISCONNECTED 상태의 표시 시간은 확정 누적값(100초)만
        assertThat(session.elapsedSeconds(T0.plusSeconds(999))).isEqualTo(100L);
    }

    @Test
    void reconnectRestoresRunningAndExcludesDisconnectedGap() {
        StudySession session = runningSessionStartedAt(T0);
        session.disconnect(T0.plusSeconds(100));      // 누적 100초

        session.reconnect(T0.plusSeconds(300));       // 200초 끊김 구간은 제외

        assertThat(session.getStatus()).isEqualTo(StudySessionStatus.RUNNING);
        assertThat(session.getResumeStatus()).isNull();
        // 재개 후 60초 추가 → 100 + 60, 끊긴 200초는 미포함
        assertThat(session.elapsedSeconds(T0.plusSeconds(360))).isEqualTo(160L);
    }

    @Test
    void disconnectIsIdempotent() {
        StudySession session = runningSessionStartedAt(T0);
        session.disconnect(T0.plusSeconds(100));
        session.disconnect(T0.plusSeconds(500));       // 두 번째는 무시돼야 함

        assertThat(session.getStatus()).isEqualTo(StudySessionStatus.DISCONNECTED);
        assertThat(session.elapsedSeconds(T0.plusSeconds(999))).isEqualTo(100L);
    }

    @Test
    void reconnectOnNonDisconnectedIsNoOp() {
        StudySession session = runningSessionStartedAt(T0);
        session.reconnect(T0.plusSeconds(50));         // RUNNING인데 reconnect → 무시

        assertThat(session.getStatus()).isEqualTo(StudySessionStatus.RUNNING);
        // lastResumedAt이 바뀌지 않아 시작부터 흐른 시간 그대로
        assertThat(session.elapsedSeconds(T0.plusSeconds(50))).isEqualTo(50L);
    }

    @Test
    void pauseFreezesTimeAndResumeContinues() {
        StudySession session = runningSessionStartedAt(T0);

        session.pause(T0.plusSeconds(120), T0.plusHours(1));   // 120초 누적 후 정지
        assertThat(session.getStatus()).isEqualTo(StudySessionStatus.PAUSED);
        assertThat(session.getPauseDeadlineAt()).isEqualTo(T0.plusHours(1));
        // 정지 중에는 시간이 흐르지 않음
        assertThat(session.elapsedSeconds(T0.plusSeconds(600))).isEqualTo(120L);

        session.resume(T0.plusSeconds(600));
        assertThat(session.getStatus()).isEqualTo(StudySessionStatus.RUNNING);
        assertThat(session.elapsedSeconds(T0.plusSeconds(630))).isEqualTo(150L); // 120 + 30
    }

    @Test
    void disconnectWhilePausedKeepsPausedTimeAndRestoresPaused() {
        StudySession session = runningSessionStartedAt(T0);
        session.pause(T0.plusSeconds(120), T0.plusHours(1));

        session.disconnect(T0.plusSeconds(300));       // PAUSED에서 끊김 → 누적 변화 없음
        assertThat(session.getStatus()).isEqualTo(StudySessionStatus.DISCONNECTED);
        assertThat(session.getResumeStatus()).isEqualTo(StudySessionStatus.PAUSED);
        assertThat(session.elapsedSeconds(T0.plusSeconds(999))).isEqualTo(120L);

        session.reconnect(T0.plusSeconds(500));         // PAUSED로 복원
        assertThat(session.getStatus()).isEqualTo(StudySessionStatus.PAUSED);
        assertThat(session.elapsedSeconds(T0.plusSeconds(999))).isEqualTo(120L);
    }

    @Test
    void terminateIsIdempotentAndKeepsFirstReason() {
        StudySession session = runningSessionStartedAt(T0);
        session.completeByUser(T0.plusSeconds(200));
        session.terminateByReconnectTimeout(T0.plusSeconds(900)); // 이미 종료 → 무시

        assertThat(session.getStatus()).isEqualTo(StudySessionStatus.COMPLETED);
        assertThat(session.getEndReason()).isEqualTo(StudySessionEndReason.USER_EXIT);
        assertThat(session.isTerminal()).isTrue();
        assertThat(session.getAccumulatedSeconds()).isEqualTo(200L);
    }
}

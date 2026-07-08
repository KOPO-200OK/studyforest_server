package com.gongsoop.studyspace.entity;

import com.gongsoop.member.entity.Member;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class StudySessionTerminationTest {

    private static final LocalDateTime STARTED_AT = LocalDateTime.of(2026, 7, 9, 10, 0);
    private static final LocalDateTime ENDED_AT = STARTED_AT.plusMinutes(5);

    @Test
    void recordsUserExitAsCompleted() {
        StudySession session = runningSession();

        session.completeByUser(ENDED_AT);

        assertThat(session.getStatus()).isEqualTo(StudySessionStatus.COMPLETED);
        assertThat(session.getEndReason()).isEqualTo(StudySessionEndReason.USER_EXIT);
        assertThat(session.getAccumulatedSeconds()).isEqualTo(300L);
    }

    @Test
    void recordsPauseTimeoutAsAutoTermination() {
        StudySession session = runningSession();

        session.terminateByPauseTimeout(ENDED_AT);

        assertThat(session.getStatus()).isEqualTo(StudySessionStatus.AUTO_TERMINATED);
        assertThat(session.getEndReason()).isEqualTo(StudySessionEndReason.PAUSE_TIMEOUT);
    }

    @Test
    void recordsReconnectTimeoutAsAutoTermination() {
        StudySession session = runningSession();

        session.terminateByReconnectTimeout(ENDED_AT);

        assertThat(session.getStatus()).isEqualTo(StudySessionStatus.AUTO_TERMINATED);
        assertThat(session.getEndReason()).isEqualTo(StudySessionEndReason.RECONNECT_TIMEOUT);
    }

    @Test
    void recordsAdminExitAsForcedTermination() {
        StudySession session = runningSession();

        session.terminateByAdmin(ENDED_AT);

        assertThat(session.getStatus()).isEqualTo(StudySessionStatus.FORCED_TERMINATED);
        assertThat(session.getEndReason()).isEqualTo(StudySessionEndReason.ADMIN_FORCE_EXIT);
    }

    private StudySession runningSession() {
        return StudySession.start(
                mock(Member.class),
                mock(StudyChannel.class),
                mock(Seat.class),
                "한국사",
                STARTED_AT
        );
    }
}

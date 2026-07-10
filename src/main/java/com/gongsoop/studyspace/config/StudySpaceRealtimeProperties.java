package com.gongsoop.studyspace.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 실시간 좌석 시스템의 TTL·주기 상수. 운영 중 조정 가능성이 크고 테스트에서 짧게
 * 줄여야 하므로 하드코딩하지 않고 외부화한다.
 */
@ConfigurationProperties(prefix = "studyspace.realtime")
public class StudySpaceRealtimeProperties {

    private Duration holdTtl = Duration.ofSeconds(8);
    private Duration presenceTtl = Duration.ofSeconds(60);
    private Duration reconnectWindow = Duration.ofMinutes(10);
    private Duration presenceStaleThreshold = Duration.ofSeconds(90);
    private Duration pauseTimeout = Duration.ofHours(1);
    private Duration dbLastSeenThrottle = Duration.ofSeconds(60);
    private Duration startupGrace = Duration.ofSeconds(30);
    private Duration sweepInterval = Duration.ofSeconds(30);

    public Duration getHoldTtl() {
        return holdTtl;
    }

    public void setHoldTtl(Duration holdTtl) {
        this.holdTtl = holdTtl;
    }

    public Duration getPresenceTtl() {
        return presenceTtl;
    }

    public void setPresenceTtl(Duration presenceTtl) {
        this.presenceTtl = presenceTtl;
    }

    public Duration getReconnectWindow() {
        return reconnectWindow;
    }

    public void setReconnectWindow(Duration reconnectWindow) {
        this.reconnectWindow = reconnectWindow;
    }

    public Duration getPresenceStaleThreshold() {
        return presenceStaleThreshold;
    }

    public void setPresenceStaleThreshold(Duration presenceStaleThreshold) {
        this.presenceStaleThreshold = presenceStaleThreshold;
    }

    public Duration getPauseTimeout() {
        return pauseTimeout;
    }

    public void setPauseTimeout(Duration pauseTimeout) {
        this.pauseTimeout = pauseTimeout;
    }

    public Duration getDbLastSeenThrottle() {
        return dbLastSeenThrottle;
    }

    public void setDbLastSeenThrottle(Duration dbLastSeenThrottle) {
        this.dbLastSeenThrottle = dbLastSeenThrottle;
    }

    public Duration getStartupGrace() {
        return startupGrace;
    }

    public void setStartupGrace(Duration startupGrace) {
        this.startupGrace = startupGrace;
    }

    public Duration getSweepInterval() {
        return sweepInterval;
    }

    public void setSweepInterval(Duration sweepInterval) {
        this.sweepInterval = sweepInterval;
    }
}

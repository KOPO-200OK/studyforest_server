package com.gongsoop.studyspace.controller;

import com.gongsoop.studyspace.dto.response.SeatStatusResponse;
import com.gongsoop.studyspace.dto.response.StudyRoomResponse;
import com.gongsoop.studyspace.dto.response.StudySessionResponse;
import com.gongsoop.studyspace.entity.StudySessionStatus;
import com.gongsoop.studyspace.service.StudySessionService;
import com.gongsoop.studyspace.service.StudySpaceQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.time.LocalDateTime;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StudySpaceController.class)
@AutoConfigureMockMvc(addFilters = false)
class StudySpaceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StudySpaceQueryService studySpaceQueryService;

    @MockitoBean
    private StudySessionService studySessionService;

    @Test
    void returnsStudyRoomsInApiEnvelope() throws Exception {
        when(studySpaceQueryService.getActiveRooms())
                .thenReturn(List.of(new StudyRoomResponse(1L, "공숲", 1)));

        mockMvc.perform(get("/api/v1/study-rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].studyRoomId").value(1))
                .andExpect(jsonPath("$.data[0].roomName").value("공숲"));
    }

    @Test
    void returnsOccupiedAndDisabledSeatStates() throws Exception {
        when(studySpaceQueryService.getSeatStatuses(3L))
                .thenReturn(List.of(
                        new SeatStatusResponse(10L, 1, true, true),
                        new SeatStatusResponse(11L, 2, false, false)
                ));

        mockMvc.perform(get("/api/v1/study-channels/3/seats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].occupied").value(true))
                .andExpect(jsonPath("$.data[1].active").value(false));
    }

    @Test
    void occupiesAndLeavesSeat() throws Exception {
        StudySessionResponse running = new StudySessionResponse(
                30L, 3L, 10L, StudySessionStatus.RUNNING,
                null,
                LocalDateTime.of(2026, 7, 8, 15, 0), null, 0L);
        StudySessionResponse completed = new StudySessionResponse(
                30L, 3L, 10L, StudySessionStatus.COMPLETED,
                com.gongsoop.studyspace.entity.StudySessionEndReason.USER_EXIT,
                LocalDateTime.of(2026, 7, 8, 15, 0),
                LocalDateTime.of(2026, 7, 8, 15, 5), 300L);
        when(studySessionService.occupySeat(3L, 10L, null, null)).thenReturn(running);
        when(studySessionService.leaveSeat(30L, null)).thenReturn(completed);

        mockMvc.perform(post("/api/v1/study-channels/3/seats/10/occupancy")
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RUNNING"))
                .andExpect(jsonPath("$.data.studySessionId").value(30));

        mockMvc.perform(delete("/api/v1/study-sessions/30/occupancy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.endReason").value("USER_EXIT"))
                .andExpect(jsonPath("$.data.accumulatedSeconds").value(300));
    }
}

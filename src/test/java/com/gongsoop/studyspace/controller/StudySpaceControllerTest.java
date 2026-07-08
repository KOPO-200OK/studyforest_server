package com.gongsoop.studyspace.controller;

import com.gongsoop.studyspace.dto.response.SeatStatusResponse;
import com.gongsoop.studyspace.dto.response.StudyRoomResponse;
import com.gongsoop.studyspace.service.StudySpaceQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StudySpaceController.class)
@AutoConfigureMockMvc(addFilters = false)
class StudySpaceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StudySpaceQueryService studySpaceQueryService;

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
}
